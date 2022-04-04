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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.http.HttpRequest;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.UserNotMappedException;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.Cookies;

import java.util.Date;
import java.util.Optional;


@Component
@AllArgsConstructor
public class ZoweJwtScheme implements AbstractAuthenticationScheme {

    private AuthSourceService authSourceService;
    private AuthConfigurationProperties configurationProperties;
    private MessageService messageService;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOWE_JWT;
    }

    @Override
    public Optional<AuthSource> getAuthSource() {
        return authSourceService.getAuthSourceFromRequest();
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        String error = null;
        String jwt = null;
        AuthSource.Parsed parsedAuthSource;
        try {
            parsedAuthSource = authSourceService.parse(authSource);
        } catch (TokenNotValidException e) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToLogMessage();
            return new ZoweJwtAuthCommand(null, null, error);
        }
        if (authSource == null || authSource.getRawSource() == null) {
            error = this.messageService.createMessage("org.zowe.apiml.gateway.security.schema.missingAuthentication").mapToLogMessage();
            return new ZoweJwtAuthCommand(null, null, error);
        }
        final Date expiration = parsedAuthSource == null ? null : parsedAuthSource.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        try {
            jwt = authSourceService.getJWT(authSource);
        } catch (UserNotMappedException | AuthenticationTokenException e) {
            error = this.messageService.createMessage(e.getMessage()).mapToLogMessage();
        }

        return new ZoweJwtAuthCommand(expirationTime, jwt, error);
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZoweJwtAuthCommand extends AuthenticationCommand {

        public static final long serialVersionUID = -885301934611866658L;
        Long expireAt;
        String jwt;
        String errorHeader;

        @Override
        public void apply(InstanceInfo instanceInfo) {

            final RequestContext context = RequestContext.getCurrentContext();
            if (jwt != null) {
                JwtCommand.setCookie(context, configurationProperties.getCookieProperties().getCookieName(), jwt);
            } else {
                JwtCommand.setErrorHeader(context, errorHeader);
            }
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            Cookies cookies = Cookies.of(request);
            if (jwt != null) {
                JwtCommand.createCookie(cookies, configurationProperties.getCookieProperties().getCookieName(), jwt);
            } else {
                JwtCommand.addErrorHeader(request, errorHeader);
            }
        }
    }
}
