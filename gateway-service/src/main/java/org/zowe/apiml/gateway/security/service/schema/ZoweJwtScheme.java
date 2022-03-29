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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.Cookies;

import java.util.Date;
import java.util.function.BiConsumer;


@Component
@AllArgsConstructor
public class ZoweJwtScheme implements AbstractAuthenticationScheme {

    private AuthSourceService authSourceService;
    private AuthConfigurationProperties configurationProperties;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOWE_JWT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        final AuthSource.Parsed parsedAuthSource = authSourceService.parse(authSource);
        final Date expiration = parsedAuthSource == null ? null : parsedAuthSource.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        return new ZoweJwtAuthCommand(expirationTime);
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZoweJwtAuthCommand extends AuthenticationCommand {

        public static final long serialVersionUID = -885301934611866658L;
        Long expireAt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();
            updateRequest((name, token) -> JwtCommand.setCookie(context, name, token));
        }

        @Override
        public void applyToRequest(HttpRequest request) {
            Cookies cookies = Cookies.of(request);
            updateRequest((name, token) -> JwtCommand.createCookie(cookies, name, token));
        }

        void updateRequest(BiConsumer<String, String> biConsumer) {
            authSourceService.getAuthSourceFromRequest().ifPresent(authSource -> {
                String jwt = authSourceService.getJWT(authSource);
                if (jwt == null) {
                    throw new AccessDeniedException("Not able to create JWT from client certificate");
                }
                AuthConfigurationProperties.CookieProperties properties = configurationProperties.getCookieProperties();
                biConsumer.accept(properties.getCookieName(), jwt);
            });
        }
    }

}
