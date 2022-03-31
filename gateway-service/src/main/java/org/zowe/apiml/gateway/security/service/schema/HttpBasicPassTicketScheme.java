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
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.CookieUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * This bean support PassTicket. Bean is responsible for getting PassTicket from
 * SAF and generating new authentication header in request.
 */
@Component
public class HttpBasicPassTicketScheme implements AbstractAuthenticationScheme {

    private final PassTicketService passTicketService;
    private final AuthSourceService authSourceService;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final String cookieName;

    public HttpBasicPassTicketScheme(
        PassTicketService passTicketService,
        AuthSourceService authSourceService,
        AuthConfigurationProperties authConfigurationProperties
    ) {
        this.passTicketService = passTicketService;
        this.authSourceService = authSourceService;
        this.authConfigurationProperties = authConfigurationProperties;
        cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
    }

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.HTTP_BASIC_PASSTICKET;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        final long before = System.currentTimeMillis();

        final AuthSource.Parsed parsedAuthSource = authSourceService.parse(authSource);

        if (authSource == null || parsedAuthSource == null) {
            return AuthenticationCommand.EMPTY;
        }

        final String applId = authentication.getApplid();
        final String userId = parsedAuthSource.getUserId();
        String passTicket;
        try {
            passTicket = passTicketService.generate(userId, applId);
        } catch (IRRPassTicketGenerationException e) {
            throw new PassTicketException(
                String.format("Could not generate PassTicket for user ID %s and APPLID %s", userId, applId), e
            );
        }
        final String encoded = Base64.getEncoder()
            .encodeToString((userId + ":" + passTicket).getBytes(StandardCharsets.UTF_8));
        final String value = "Basic " + encoded;

        final long expiredAt = Math.min(before + authConfigurationProperties.getPassTicket().getTimeout() * 1000,
            parsedAuthSource.getExpiration().getTime());

        return new PassTicketCommand(value, cookieName, expiredAt);
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class PassTicketCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 3941300386857998443L;

        private static final String COOKIE_HEADER = "cookie";

        private final String authorizationValue;
        private final String cookieName;
        private final long expireAt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, authorizationValue);
            context.addZuulRequestHeader(COOKIE_HEADER,
                CookieUtil.removeCookie(
                    context.getZuulRequestHeaders().get(COOKIE_HEADER),
                    cookieName
                )
            );
        }

        @Override
        public boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        @Override
        public boolean isRequiredValidSource() {
            return true;
        }

    }
}
