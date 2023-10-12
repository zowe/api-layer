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

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource.Origin;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of AuthSourceService which supports JWT token as authentication source.
 */
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@RequiredArgsConstructor
public class JwtAuthSourceService extends TokenAuthSourceService {
    @InjectApimlLogger
    protected final ApimlLogger logger = ApimlLogger.empty();

    private final AuthenticationService authenticationService;

    @Override
    protected ApimlLogger getLogger() {
        return logger;
    }

    @Override
    public Function<String, AuthSource> getMapper() {
        return JwtAuthSource::new;
    }

    @Override
    public Optional<String> getToken(HttpServletRequest request) {
        Optional<String> tokenOptional = authenticationService.getJwtTokenFromRequest(request);
        if (tokenOptional.isPresent()) {
            AuthSource.Origin origin = authenticationService.getTokenOrigin(tokenOptional.get());
            if (Origin.ZOSMF == origin || Origin.ZOWE == origin) {
                return tokenOptional;
            }
        }
        return Optional.empty();
    }

    /**
     * Validates authentication source (JWT token) using method of {@link AuthenticationService}
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token)
     * @return true if token is valid, false otherwise
     */
    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource) authSource).getRawSource();
            logger.log(MessageType.DEBUG, "Validating JWT token.");
            return jwtToken != null && authenticationService.validateJwtToken(jwtToken).isAuthenticated();
        }
        return false;
    }

    /**
     * Validates authentication source (JWT token) using method of {@link AuthenticationService}
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token)
     * @return authentication source in parsed form
     */
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource) authSource).getRawSource();
            logger.log(MessageType.DEBUG, "Parsing JWT token.");
            QueryResponse queryResponse = jwtToken == null ? null : authenticationService.parseJwtToken(jwtToken);
            return queryResponse == null ? null : new ParsedTokenAuthSource(queryResponse.getUserId(), queryResponse.getCreation(), queryResponse.getExpiration(),
                Origin.valueByTokenSource(queryResponse.getSource()));
        }
        return null;
    }

    /**
     * Generates LTPA token from current source of authentication (JWT token) using method of {@link AuthenticationService}
     *
     * @param authSource {@link AuthSource} object which hold original source of authentication (JWT token)
     * @return LTPA token
     */
    public String getLtpaToken(AuthSource authSource) {
        if (authSource instanceof JwtAuthSource) {
            String jwtToken = ((JwtAuthSource) authSource).getRawSource();
            return jwtToken == null ? null : authenticationService.getLtpaTokenWithValidation(jwtToken);
        }
        return null;
    }

    @Override
    public String getJWT(AuthSource authSource) {
        return ((JwtAuthSource) authSource).getRawSource();
    }
}
