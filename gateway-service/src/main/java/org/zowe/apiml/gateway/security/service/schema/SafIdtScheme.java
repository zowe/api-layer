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

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.CookieUtil;

import io.jsonwebtoken.Claims;

import javax.annotation.PostConstruct;

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
    private String cookieName;

    @PostConstruct
    public void initCookieName() {
        cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        final AuthSource.Parsed parsedAuthSource = authSourceService.parse(authSource);

        if (parsedAuthSource == null) {
            return AuthenticationCommand.EMPTY;
        }

        final String userId = parsedAuthSource.getUserId();
        final String applId = authentication.getApplid();
        if (applId == null) {
            throw new PassTicketException(
                    "Applid is required. Check the configuration of service"
            );
        }

        String safIdentityToken;
        try {
            char[] passTicket = passTicketService.generate(userId, applId).toCharArray();
            try {
                safIdentityToken = safIdtProvider.generate(userId, passTicket, applId);
            } finally {
                Arrays.fill(passTicket, (char) 0);
            }
        } catch (IRRPassTicketGenerationException e) {
            throw new PassTicketException(
                    String.format("Could not generate PassTicket for user ID '%s' and APPLID '%s'", userId, applId), e
            );
        }

        try {
            Claims claims = getJwtClaims(safIdentityToken);
            Date expirationDate = claims.getExpiration();
            if (expirationDate == null) {
                expirationDate = DateUtils.addMinutes(new Date(), defaultIdtExpiration);
            }

            return new SafIdtCommand(safIdentityToken, cookieName, expirationDate.getTime());
        } catch (TokenNotValidException | TokenExpireException e) {
            throw new SafIdtException("Unable to parse Identity Token", e);
        }
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    @RequiredArgsConstructor
    public class SafIdtCommand extends AuthenticationCommand {
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
