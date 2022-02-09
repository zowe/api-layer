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
import lombok.RequiredArgsConstructor;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.util.CookieUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.function.Supplier;

import static org.zowe.apiml.gateway.security.service.AuthenticationUtils.getJwtClaims;

/**
 * The scheme allowing for the safIdt authentication scheme.
 * It adds new header with the SAF IDT token in case of valid JWT provided.
 */
@Component
@RequiredArgsConstructor
public class SafIdtScheme implements AbstractAuthenticationScheme {

    private final AuthConfigurationProperties authConfigurationProperties;
    private final PassTicketService passTicketService;
    private final SafIdtProvider safIdtProvider;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> tokenSupplier) {
        final QueryResponse zoweToken = tokenSupplier.get();

        if (zoweToken == null) {
            return AuthenticationCommand.EMPTY;
        }

        final String userId = zoweToken.getUserId();
        char[] passTicket = new char[0];
        final String applId = authentication.getApplid();

        String safIdentityToken;
        try {
            passTicket = passTicketService.generate(userId, applId).toCharArray();
            safIdentityToken = safIdtProvider.generate(userId, passTicket, applId);
        } catch (IRRPassTicketGenerationException e) {
            throw new PassTicketException(
                    String.format("Could not generate PassTicket for user ID '%s' and APPLID '%s'", userId, applId), e
            );
        } finally {
            Arrays.fill(passTicket, (char) 0);
        }

        Claims claims = getJwtClaims(safIdentityToken);
        Date expirationDate = claims.getExpiration();
        if (expirationDate == null)
            throw new TokenExpireException("Token is expired.");

        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();

        return new SafIdtCommand(safIdentityToken, cookieName, expirationDate.getTime());
    }

    @RequiredArgsConstructor
    public static class SafIdtCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 8213192949049438897L;

        private final String safIdentityToken;
        private final String cookieName;
        private final Long expireAt;

        private static final String COOKIE_HEADER = "cookie";
        private static final String SAF_TOKEN_HEADER = "X-SAF-Token";

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            context.addZuulRequestHeader(SAF_TOKEN_HEADER, safIdentityToken);
            context.addZuulRequestHeader(COOKIE_HEADER,
                    CookieUtil.removeCookie(
                            context.getZuulRequestHeaders().get(COOKIE_HEADER),
                            cookieName
                    )
            );
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            request.setHeader(
                    new BasicHeader(SAF_TOKEN_HEADER, safIdentityToken)
            );
            Header header = request.getFirstHeader(COOKIE_HEADER);
            if (header != null) {
                request.setHeader(COOKIE_HEADER,
                        CookieUtil.removeCookie(
                                header.getValue(),
                                cookieName
                        )
                );
            }
        }

        @Override
        public boolean isExpired() {
            if (expireAt == null) return false;

            return System.currentTimeMillis() > expireAt;
        }

        @Override
        public boolean isRequiredValidJwt() {
            return true;
        }
    }

}
