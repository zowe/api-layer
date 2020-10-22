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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;

import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.LTPA;

/**
 * Authentication provider that verifies credentials against z/OSMF service
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ZosmfAuthenticationProvider implements AuthenticationProvider {

    @Value("${apiml.security.zosmf.useJwtToken:true}")
    private boolean useJwtToken;

    private final AuthenticationService authenticationService;
    private final ZosmfService zosmfService;

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

        log.error(ar.toString());


        // if z/OSMF support JWT, use it as Zowe JWT token
        if (ar.getTokens().containsKey(JWT) && useJwtToken) {
            log.error(ar.getDomain());
            return authenticationService.createTokenAuthentication(user, ar.getTokens().get(JWT));
        }

        if (ar.getTokens().containsKey(LTPA)) {
            // construct own JWT token, including LTPA from z/OSMF
            final String domain = ar.getDomain();
            log.error(domain);
            final String jwtToken = authenticationService.createJwtToken(user, domain, ar.getTokens().get(LTPA));
            log.error(jwtToken);
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
