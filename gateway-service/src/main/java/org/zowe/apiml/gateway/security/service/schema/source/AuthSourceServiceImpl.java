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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.QueryResponse;

/**
 * Main implementation of AuthSourceService, currently support only on type of authentication source - JWT token.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthSourceServiceImpl implements AuthSourceService {
    @Autowired
    private AuthenticationService authenticationService;

    public Optional<AuthSource> getAuthSourceFromRequest() {
        final RequestContext context = RequestContext.getCurrentContext();

        Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
        return jwtToken.map(JwtAuthSource::new);
    }

    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource)authSource).getRawSource();
            return jwtToken != null && authenticationService.validateJwtToken(jwtToken).isAuthenticated();
        }
        return false;
    }

    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource)authSource).getRawSource();
            QueryResponse queryResponse = jwtToken == null ? null : authenticationService.parseJwtToken(jwtToken);
            return queryResponse == null ? null : new JwtAuthSource.Parsed(queryResponse.getUserId(), queryResponse.getCreation(), queryResponse.getExpiration(),
                queryResponse.getSource());
        }
        return null;
    }

    public String getLtpaToken(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource)authSource).getRawSource();
            return jwtToken == null ? null : authenticationService.getLtpaTokenWithValidation(jwtToken);
        }
        return null;
    }
}
