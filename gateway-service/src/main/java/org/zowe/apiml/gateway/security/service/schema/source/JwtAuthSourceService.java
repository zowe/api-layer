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
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.security.common.token.QueryResponse;

/**
 * Implementation of AuthSourceService which supports JWT token as authentication source.
 */
@Slf4j
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class JwtAuthSourceService implements AuthSourceService {
    @Autowired
    private AuthenticationService authenticationService;

    /**
     * Core method of the interface. Gets source of authentication (JWT token) from request.
     * <p>
     * @return Optional<AuthSource> which hold original source of authentication (JWT token)
     * or Optional.empty() when no authentication source found.
     */
    public Optional<AuthSource> getAuthSourceFromRequest() {
        final RequestContext context = RequestContext.getCurrentContext();

        Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
        return jwtToken.map(JwtAuthSource::new);
    }

    /**
     * Validates authentication source (JWT token) using method of {@link AuthenticationService}
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token)
     * @return true if token is valid, false otherwise
     */
    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource)authSource).getRawSource();
            return jwtToken != null && authenticationService.validateJwtToken(jwtToken).isAuthenticated();
        }
        return false;
    }

    /**
     * Validates authentication source (JWT token) using method of {@link AuthenticationService}
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token)
     * @return authentication source in parsed form
     */
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource)authSource).getRawSource();
            QueryResponse queryResponse = jwtToken == null ? null : authenticationService.parseJwtToken(jwtToken);
            return queryResponse == null ? null : new JwtAuthSource.Parsed(queryResponse.getUserId(), queryResponse.getCreation(), queryResponse.getExpiration(),
                Origin.valueByIssuer(queryResponse.getSource().name()));
        }
        return null;
    }

    /**
     * Generates LTPA token from current source of authentication (JWT token) using method of {@link AuthenticationService}
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token)
     * @return LTPA token
     */
    public String getLtpaToken(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource)authSource).getRawSource();
            return jwtToken == null ? null : authenticationService.getLtpaTokenWithValidation(jwtToken);
        }
        return null;
    }

    @Override
    public String getJWT(AuthSource authSource) {
        return ((JwtAuthSource)authSource).getRawSource();
    }
}
