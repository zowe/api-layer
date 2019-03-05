/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.security.token;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
public class TokenAuthenticationProviderTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private TokenService tokenService;

    @Before
    public void setUp() {
        tokenService = mock(TokenService.class);
    }

    @Test
    public void authenticateWithValidToken() {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("token");
        when(tokenService.validateToken(tokenAuthentication)).thenReturn(tokenAuthentication);

        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider(tokenService);
        Authentication authentication = authenticationProvider.authenticate(tokenAuthentication);

        assertThat(authentication, is(tokenAuthentication));
    }

    @Test
    public void authenticateWithNotValidToken() {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("token");
        BadCredentialsException badCredentials = new BadCredentialsException("bad token");
        when(tokenService.validateToken(tokenAuthentication)).thenThrow(badCredentials);
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("bad token");

        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider(tokenService);
        authenticationProvider.authenticate(tokenAuthentication);
    }

    @Test
    public void supportsAuthentication() {
        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider(tokenService);

        assertTrue(authenticationProvider.supports(TokenAuthentication.class));
        assertFalse(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class));
    }
}
