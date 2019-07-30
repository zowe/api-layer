/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.login;

import com.ca.apiml.security.service.GatewaySecurityService;
import com.ca.apiml.security.token.TokenAuthentication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Authentication provider that authenticates UsernamePasswordAuthenticationToken against Gateway
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayLoginProvider implements AuthenticationProvider {
    private final GatewaySecurityService gatewaySecurityService;

    /**
     * Authenticate the credentials
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getPrincipal().toString();

        Optional<String> token = gatewaySecurityService.login(username, authentication.getCredentials().toString());

        if (!token.isPresent()) {
            throw new BadCredentialsException("Username or password are invalid.");
        }

        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token.get());
        tokenAuthentication.setAuthenticated(true);

        return tokenAuthentication;
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth.equals(UsernamePasswordAuthenticationToken.class);
    }
}
