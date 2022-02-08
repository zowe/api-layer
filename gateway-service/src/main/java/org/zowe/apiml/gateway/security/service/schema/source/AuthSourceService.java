/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.schema.source;

import com.netflix.zuul.context.RequestContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.QueryResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthSourceService {
    @Autowired
    private AuthenticationService authenticationService;


    public Optional<JwtAuthSource> getAuthSource() {
        final RequestContext context = RequestContext.getCurrentContext();

        String jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest()).orElse(null);
        if (jwtToken != null) {
            return Optional.of(new JwtAuthSource(jwtToken));
        }

        return Optional.empty();
    }

    public boolean isValid(JwtAuthSource authSource) {
        return authenticationService.validateJwtToken(authSource.getSource()).isAuthenticated();
    }

    public QueryResponse parse(JwtAuthSource authSource) {
        return authenticationService.parseJwtToken(authSource.getSource());
    }
}
