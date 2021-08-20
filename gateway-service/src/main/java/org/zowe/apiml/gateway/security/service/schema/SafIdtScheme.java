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
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The scheme allowing for the safIdt authentication scheme.
 * It adds new header with the SAF IDT token in case of valid JWT provided.
 */
@Component
@RequiredArgsConstructor
public class SafIdtScheme implements AbstractAuthenticationScheme {
    private final AuthenticationService authenticationService;
    private final SafIdtProvider safIdtProvider;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, Supplier<QueryResponse> tokenSupplier) {
        // Same behavior as for the ZosmfScheme.
        final QueryResponse queryResponse = tokenSupplier.get();
        final Date expiration = queryResponse == null ? null : queryResponse.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        return new SafIdtCommand(expirationTime);
    }

    @RequiredArgsConstructor
    public class SafIdtCommand extends AuthenticationCommand {
        private final Long expireAt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();

            Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
            jwtToken.ifPresent(token -> {
                TokenAuthentication authentication = authenticationService.validateJwtToken(jwtToken.get());
                if (authentication.isAuthenticated()) {
                    // Get principal from the token?
                    Optional<String> safIdt = safIdtProvider.generate(authentication.getPrincipal());

                    safIdt.ifPresent(safToken -> context.addZuulRequestHeader("X-SAF-Token", safToken));
                }
            });
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
