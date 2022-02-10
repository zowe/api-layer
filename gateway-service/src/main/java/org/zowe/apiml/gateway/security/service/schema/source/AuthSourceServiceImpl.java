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
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Parsed;
import org.zowe.apiml.security.common.token.QueryResponse;

/**
 * Main implementation of AuthSourceService, currently support only on type of authentication source - JWT token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthSourceServiceImpl implements AuthSourceService {
    @Autowired
    private AuthenticationService authenticationService;

    public Optional<AuthSource> getAuthSource() {
        final RequestContext context = RequestContext.getCurrentContext();

        String jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest()).orElse(null);
        if (jwtToken != null) {
            return Optional.of(new JwtAuthSource(jwtToken));
        }

        return Optional.empty();
    }

    public boolean isValid(AuthSource authSource) {
        return authenticationService.validateJwtToken(((JwtAuthSource)authSource).getSource()).isAuthenticated();
    }

    public AuthSource.Parsed parse(AuthSource authSource) {
        QueryResponse queryResponse = authenticationService.parseJwtToken(((JwtAuthSource)authSource).getSource());
        return new Parsed(queryResponse.getUserId(), queryResponse.getCreation(), queryResponse.getExpiration(), queryResponse.getSource());
    }

    public String getLtpaToken(AuthSource authSource) {
        return authenticationService.getLtpaTokenWithValidation((String)authSource.getSource());
    }
}
