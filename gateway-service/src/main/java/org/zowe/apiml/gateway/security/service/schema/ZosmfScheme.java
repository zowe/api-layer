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

import org.zowe.apiml.security.common.auth.Authentication;
import org.zowe.apiml.security.common.auth.AuthenticationScheme;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

/**
 * This bean provide LTPA token into request. It get LTPA from JWT token (value is set on logon) and distribute it as
 * cookie.
 */
@Component
@AllArgsConstructor
public class ZosmfScheme implements AbstractAuthenticationScheme {

    private final AuthenticationService authenticationService;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.ZOSMF;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, QueryResponse token) {
        final Date expiration = token == null ? null : token.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        return new ZosmfCommand(expirationTime);
    }

    @lombok.Value
    @EqualsAndHashCode(callSuper = false)
    public class ZosmfCommand extends AuthenticationCommand {

        private static final long serialVersionUID = 2284037230674275720L;

        public static final String COOKIE_HEADER = "cookie";

        private final Long expireAt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();

            Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
            jwtToken.ifPresent(token -> {
                String ltpaToken = authenticationService.getLtpaTokenFromJwtToken(token);

                String cookie = context.getZuulRequestHeaders().get(COOKIE_HEADER);
                if (cookie != null) {
                    cookie += "; " + ltpaToken;
                } else {
                    cookie = ltpaToken;
                }

                context.addZuulRequestHeader(COOKIE_HEADER, cookie);
            });
        }

        @Override
        public boolean isExpired() {
            if (expireAt == null) return false;

            return System.currentTimeMillis() > expireAt;
        }
    }

}
