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
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;

import java.util.Optional;

/**
 * The scheme allowing for the safIdt authentication scheme.
 * It adds new header with the SAF IDT token in case of valid authentication source provided.
 */
@Component
@RequiredArgsConstructor
public class SafIdtScheme implements AbstractAuthenticationScheme {
    private final AuthSourceService authSourceService;
    private final SafIdtProvider safIdtProvider;

    @Override
    public AuthenticationScheme getScheme() {
        return AuthenticationScheme.SAF_IDT;
    }

    @Override
    public AuthenticationCommand createCommand(Authentication authentication, AuthSource authSource) {
        // Same behavior as for the ZosmfScheme.
        final AuthSource.Parsed parsedSource = authSourceService.parse(authSource);
        final Date expiration = parsedSource == null ? null : parsedSource.getExpiration();
        final Long expirationTime = expiration == null ? null : expiration.getTime();
        return new SafIdtCommand(expirationTime);
    }

    @RequiredArgsConstructor
    public class SafIdtCommand extends AuthenticationCommand {
        private final Long expireAt;

        @Override
        public void apply(InstanceInfo instanceInfo) {
            final RequestContext context = RequestContext.getCurrentContext();

            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest();
            authSource.ifPresent(token -> {
                if (authSourceService.isValid(token)) {
                    AuthSource.Parsed parsedSource = authSourceService.parse(token);
                    if (parsedSource != null) {
                        Optional<String> safIdt = safIdtProvider.generate(parsedSource.getUserId());

                        safIdt.ifPresent(safToken -> context.addZuulRequestHeader("X-SAF-Token", safToken));
                    }
                }
            });
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

        @Override
        public boolean isValidSource(AuthSource authSource) {
            return authSourceService.isValid(authSource);
        }
    }
}
