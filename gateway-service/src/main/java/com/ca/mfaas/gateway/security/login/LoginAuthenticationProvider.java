/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.login;

import com.ca.mfaas.gateway.security.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.token.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoginAuthenticationProvider extends DaoAuthenticationProvider {
    private final TokenService tokenService;

    public LoginAuthenticationProvider(BCryptPasswordEncoder encoder,
                                       @Qualifier("inMemoryUserDetailsService") UserDetailsService userDetailsService,
                                       TokenService tokenService) {
        super();
        this.setPasswordEncoder(encoder);
        this.setUserDetailsService(userDetailsService);
        this.tokenService = tokenService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication;
        try {
            usernamePasswordAuthentication
                = (UsernamePasswordAuthenticationToken) super.authenticate(authentication);
        } catch (AuthenticationException exception) {
            throw new InvalidUserException("Username or password are invalid");
        } catch (Exception e) {
            throw new AuthenticationServiceException("A failure occurred when authenticating", e);
        }
        String username = usernamePasswordAuthentication.getName();
        String token = tokenService.createToken(username);
        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token);
        tokenAuthentication.setAuthenticated(true);
        return tokenAuthentication;
    }

}
