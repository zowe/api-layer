/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.login;

import com.ca.mfaas.security.token.TokenAuthentication;
import com.ca.mfaas.security.token.TokenService;
import com.ca.mfaas.security.user.FileUserDetails;
import com.ca.mfaas.security.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoginAuthenticationProviderTest {
    private BCryptPasswordEncoder encoder = mock(BCryptPasswordEncoder.class);
    private UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private TokenService tokenService = mock(TokenService.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        encoder = mock(BCryptPasswordEncoder.class);
        userDetailsService = mock(UserDetailsService.class);
        tokenService = mock(TokenService.class);
    }

    @Test
    public void loginWithExistingUser() {
        String username = "user";
        String password = "password";
        String token = "token";
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication = new UsernamePasswordAuthenticationToken(username, password);
        UserDetails userDetails = new FileUserDetails(new User(username, password));

        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(encoder.matches(password, password)).thenReturn(true);
        when(tokenService.createToken(username)).thenReturn(token);

        LoginAuthenticationProvider loginAuthenticationProvider
            = new LoginAuthenticationProvider(encoder, userDetailsService, tokenService);
        TokenAuthentication tokenAuthentication
            = (TokenAuthentication) loginAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        assertThat(tokenAuthentication.isAuthenticated(), is(true));
        assertThat(tokenAuthentication.getCredentials(), is(token));
    }

    @Test
    public void loginWithNotExistingUser() {
        String username = "not-existing-user";
        String password = "some-password";
        UsernamePasswordAuthenticationToken usernamePasswordAuthentication
            = new UsernamePasswordAuthenticationToken(username, password);

        when(userDetailsService.loadUserByUsername(username)).thenThrow(new UsernameNotFoundException("User not found"));
        exception.expect(InvalidUserException.class);
        exception.expectMessage("Username or password are invalid");

        LoginAuthenticationProvider loginAuthenticationProvider
            = new LoginAuthenticationProvider(encoder, userDetailsService, tokenService);
        loginAuthenticationProvider.authenticate(usernamePasswordAuthentication);

        verify(tokenService, never()).createToken(any());
        verify(userDetailsService).loadUserByUsername(username);
    }

}
