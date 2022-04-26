/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.CookieUtil;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.zowe.apiml.gateway.filters.pre.ServiceAuthenticationFilter.AUTH_FAIL_HEADER;
import static org.zowe.apiml.gateway.security.service.JwtUtils.getJwtClaims;

/**
 * The scheme allowing for the safIdt authentication scheme.
 * It adds new header with the SAF IDT token in case of valid authentication source provided.
 */
@Component
@RequiredArgsConstructor
public class SafIdtScheme implements IAuthenticationScheme {
    private final AuthConfigurationProperties authConfigurationProperties;
    private final AuthSourceService authSourceService;
    private final PassTicketService passTicketService;
    private final SafIdtProvider safIdtProvider;
    private final MessageService messageService;

    @Value("${apiml.security.saf.defaultIdtExpiration:10}")
    int defaultIdtExpiration;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        String error = null;

        final RequestContext context = RequestContext.getCurrentContext();
        // Check for error in context to use it in header "X-Zowe-Auth-Failure"
        if (context.containsKey(AUTH_FAIL_HEADER)) {
            error = context.get(AUTH_FAIL_HEADER).toString();
            // this command should expire immediately after creation because it is build based on missing/incorrect authentication
            return new SafIdtCommand(null, null, error);
        }

        // check the authentication source
        if (authSource == null || authSource.getRawSource() == null) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.schema.missingAuthentication").mapToLogMessage();
            return new SafIdtCommand(null, null, error);
        }
        // parse the authentication source
        AuthSource.Parsed parsedAuthSource = null;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
        } catch (TokenNotValidException e) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToLogMessage();
        } catch (TokenExpireException e) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.expiredToken").mapToLogMessage();
        }
        if (parsedAuthSource == null) {
            return new SafIdtCommand(null, null, error);
        }

        String safIdentityToken;
        long expireAt;
        try {
            String applId = getApplId(authentication);
            safIdentityToken = generateSafIdentityToken(parsedAuthSource, applId);
            expireAt = getSafIdtExpiration(safIdentityToken);
        } catch (SafIdtSchemeException e) {
            return new SafIdtCommand(null, null, e.getMessage());
        }

        return new SafIdtCommand(safIdentityToken, expireAt, error);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    private String getApplId(Authentication authentication) throws SafIdtSchemeException {
        String error;
        String applId = authentication == null ? null : authentication.getApplid();
        if (applId == null) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.scheme.missingApplid").mapToLogMessage();
            throw new SafIdtSchemeException(error);
        }
        return applId;
    }

    private String generateSafIdentityToken(@NotNull AuthSource.Parsed parsedAuthSource , @NotNull String applId) throws SafIdtSchemeException {
        String safIdentityToken = null;
        String error;

        String userId = parsedAuthSource.getUserId();
        if (userId == null) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.schema.x509.mappingFailed").mapToLogMessage();
            throw new SafIdtSchemeException(error);
        }

        char[] passTicket = "".toCharArray();
        try {
            passTicket = passTicketService.generate(userId, applId).toCharArray();
            safIdentityToken = safIdtProvider.generate(userId, passTicket, applId);
        } catch (IRRPassTicketGenerationException e) {
            error = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", e.getMessage()).mapToLogMessage();
            throw new SafIdtSchemeException(error);
        } catch (SafIdtException | SafIdtAuthException e) {
            error = messageService.createMessage("org.zowe.apiml.security.idt.failed", e.getMessage()).mapToLogMessage();
            throw new SafIdtSchemeException(error);
        } finally {
            Arrays.fill(passTicket, (char) 0);
        }
        return safIdentityToken;
    }

    private long getSafIdtExpiration(String safIdentityToken) throws SafIdtSchemeException {
        Date expirationTime;
        String error;
        try {
            Claims claims = getJwtClaims(safIdentityToken);
            expirationTime = claims.getExpiration();
            if (expirationTime == null) {
                expirationTime = DateUtils.addMinutes(new Date(), defaultIdtExpiration);
            }
        } catch (TokenNotValidException e) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToLogMessage();
            throw new SafIdtSchemeException(error);
        } catch (TokenExpireException e) {
            error = messageService.createMessage("org.zowe.apiml.gateway.security.expiredToken").mapToLogMessage();
            throw new SafIdtSchemeException(error);
        }
        return expirationTime.getTime();
    }

    @RequiredArgsConstructor
    public class SafIdtCommand extends AuthenticationCommand {
        private static final long serialVersionUID = 8213192949049438897L;

        @Getter
        private final String safIdentityToken;
        @Getter
        private final Long expireAt;
        @Getter
        private final String errorMessage;

        protected static final String SAF_TOKEN_HEADER = "X-SAF-Token";

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            if (safIdentityToken != null && errorMessage == null) {
                // add header with SafIdt token to request and remove APIML token from Cookie if exists
                context.addZuulRequestHeader(SAF_TOKEN_HEADER, safIdentityToken);
                JwtCommand.removeCookie(context, authConfigurationProperties.getCookieProperties().getCookieName());
            }
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            if (safIdentityToken != null) {
                // add header with SafIdt token to request and remove APIML token from Cookie if exists
                request.setHeader(
                    new BasicHeader(SAF_TOKEN_HEADER, safIdentityToken)
                );
                Header header = request.getFirstHeader(JwtCommand.COOKIE_HEADER);
                if (header != null) {
                    request.setHeader(JwtCommand.COOKIE_HEADER,
                        CookieUtil.removeCookie(
                            header.getValue(),
                            authConfigurationProperties.getCookieProperties().getCookieName()
                        )
                    );
                }
            }
        }

        @Override
        public boolean isExpired() {
            if (expireAt == null) return false;

            return System.currentTimeMillis() > expireAt;
        }

        @Override
        public boolean isRequiredValidSource() {
            return true;
        }
    }
}
