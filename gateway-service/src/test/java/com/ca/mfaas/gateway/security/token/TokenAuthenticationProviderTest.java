/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.security.token;

import com.ca.apiml.security.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.query.TokenAuthenticationProvider;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TokenAuthenticationProviderTest {
    private AuthenticationService tokenService;


    @Before
    public void setUp() {
        tokenService = mock(AuthenticationService.class);
    }

    @Test
    public void authenticateWithValidToken() {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("token");

        when(tokenService.validateJwtToken(tokenAuthentication)).thenReturn(tokenAuthentication);
        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider(tokenService);
        Authentication authentication = authenticationProvider.authenticate(tokenAuthentication);

        assertThat(authentication, is(tokenAuthentication));
    }


    @Test
    public void supportsAuthentication() {
        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider(tokenService);

        assertTrue(authenticationProvider.supports(TokenAuthentication.class));
        assertFalse(authenticationProvider.supports(UsernamePasswordAuthenticationToken.class));
    }
}
