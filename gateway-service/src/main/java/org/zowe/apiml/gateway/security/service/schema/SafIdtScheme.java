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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.adapter.VersionAdapterUtils;
import org.zowe.apiml.gateway.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

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

    @Value("${apiml.security.saf.defaultIdtExpiration:10}")
    int defaultIdtExpiration;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        // check the authentication source
        if (authSource == null || authSource.getRawSource() == null) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.schema.missingAuthentication");
        }
        // parse the authentication source
        AuthSource.Parsed parsedAuthSource;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
            if (parsedAuthSource == null) {
                throw new IllegalStateException("Error occurred while parsing authenticationSource");
            }
        } catch (TokenNotValidException e) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.invalidToken");
        } catch (TokenExpireException e) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.expiredToken");
        }

        String safIdentityToken;
        long expireAt;

        String applId = getApplId(authentication);
        safIdentityToken = generateSafIdentityToken(parsedAuthSource, applId);
        expireAt = getSafIdtExpiration(safIdentityToken);

        return new SafIdtCommand(safIdentityToken, expireAt);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest(VersionAdapterUtils.toJakarta(RequestContext.getCurrentContext().getRequest()));
    }

    private String getApplId(Authentication authentication) {
        String applId = authentication == null ? null : authentication.getApplid();
        if (applId == null) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.scheme.missingApplid");
        }
        return applId;
    }

    private String generateSafIdentityToken(@NotNull AuthSource.Parsed parsedAuthSource, @NotNull String applId) {
        String safIdentityToken;

        String userId = parsedAuthSource.getUserId();
        if (userId == null) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.schema.x509.mappingFailed");
        }

        char[] passTicket = "".toCharArray();
        try {
            passTicket = passTicketService.generate(userId, applId).toCharArray();
            safIdentityToken = safIdtProvider.generate(userId, passTicket, applId);
        } catch (IRRPassTicketGenerationException e) {
            throw new AuthSchemeException("org.zowe.apiml.security.ticket.generateFailed", e.getMessage());
        } catch (SafIdtException | SafIdtAuthException e) {
            throw new AuthSchemeException("org.zowe.apiml.security.idt.failed", e.getMessage());
        } finally {
            Arrays.fill(passTicket, (char) 0);
        }
        return safIdentityToken;
    }

    private long getSafIdtExpiration(String safIdentityToken) {
        Date expirationTime;
        try {
            Claims claims = getJwtClaims(safIdentityToken);
            expirationTime = claims.getExpiration();
            if (expirationTime == null) {
                expirationTime = DateUtils.addMinutes(new Date(), defaultIdtExpiration);
            }
        } catch (TokenNotValidException e) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.invalidToken");
        } catch (TokenExpireException e) {
            throw new AuthSchemeException("org.zowe.apiml.gateway.security.expiredToken");
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


        @Override
        public void apply(InstanceInfo instanceInfo) {
            if (safIdentityToken != null) {
                final RequestContext context = RequestContext.getCurrentContext();
                // add header with SafIdt token to request and remove APIML token from Cookie if exists
                context.addZuulRequestHeader(ApimlConstants.SAF_TOKEN_HEADER, safIdentityToken);
                String[] cookiesToBeRemoved = new String[]{authConfigurationProperties.getCookieProperties().getCookieName(), authConfigurationProperties.getCookieProperties().getCookieNamePAT()};
                JwtCommand.removeCookie(context, cookiesToBeRemoved);
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
