/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.zosmf;

import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import static com.ca.mfaas.gateway.security.service.ZosmfService.TokenType.JWT;
import static com.ca.mfaas.gateway.security.service.ZosmfService.TokenType.LTPA;

/**
 * Authentication provider that verifies credentials against z/OSMF service
 */
@Component
public class ZosmfAuthenticationProvider implements AuthenticationProvider {

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private final AuthenticationService authenticationService;
    private final ZosmfService zosmfService;

    public ZosmfAuthenticationProvider(
        AuthenticationService authenticationService,
        ZosmfService zosmfService
    ) {
        this.authenticationService = authenticationService;
        this.zosmfService = zosmfService;
    }

    /**
     * Authenticate the credentials with the z/OSMF service
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        final String user = authentication.getPrincipal().toString();

        final ZosmfService.AuthenticationResponse ar = zosmfService.authenticate(authentication);

        // if z/OSMF support JWT, use it as Zowe JWT token
        if (ar.getTokens().containsKey(JWT)) {
            return authenticationService.createTokenAuthentication(user, ar.getTokens().get(JWT));
        }

        if (ar.getTokens().containsKey(LTPA)) {
            // construct own JWT token, including LTPA from z/OSMF
            final String domain = ar.getDomain();
            final String jwtToken = authenticationService.createJwtToken(user, domain, ar.getTokens().get(LTPA));

            return authenticationService.createTokenAuthentication(user, jwtToken);
        }

        // JWT and LTPA tokens are missing, authentication was wrong
        throw new BadCredentialsException("Username or password are invalid.");
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth == UsernamePasswordAuthenticationToken.class;
    }

}
