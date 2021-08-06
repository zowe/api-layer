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
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.SafAuthenticationService;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The scheme allowing for the safIdt authentication scheme.
 *
 */
@Component
@RequiredArgsConstructor
public class SafIdtScheme implements AbstractAuthenticationScheme {
    private final AuthenticationService authenticationService;
    private final SafAuthenticationService safAuthenticationService;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> token) {
        return new SafIdtCommand();
    }

    public class SafIdtCommand extends AuthenticationCommand {

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();

            Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
            jwtToken.ifPresent(token -> {
                String safIdt = safAuthenticationService.generateSafIdt(token);

                // remove authentication part
                context.addZuulRequestHeader(HttpHeaders.AUTHORIZATION, safIdt);
            });
        }

        @Override
        public boolean isExpired() {
            // Verify whether the JWT token is expired.
            return false;
        }

        @Override
        public boolean isRequiredValidJwt() {
            return true;
        }
    }
}
