/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.config;

import com.ca.mfaas.gateway.security.login.LoginProvider;
import com.ca.mfaas.gateway.security.login.dummy.DummyAuthenticationProvider;
import com.ca.mfaas.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import com.ca.mfaas.gateway.security.token.TokenAuthenticationProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AuthProviderInitializerTest {

    private DummyAuthenticationProvider dummyAuthenticationProvider;
    private TokenAuthenticationProvider tokenAuthenticationProvider;
    private ZosmfAuthenticationProvider zosmfAuthenticationProvider;

    @Before
    public void setup() {
        dummyAuthenticationProvider = mock(DummyAuthenticationProvider.class);
        tokenAuthenticationProvider = mock(TokenAuthenticationProvider.class);
        zosmfAuthenticationProvider = mock(ZosmfAuthenticationProvider.class);
    }

    @Test
    public void testDummyProvider() {
        String authProvider = LoginProvider.DUMMY.toString();

        AuthProviderInitializer authProviderInitializer = new AuthProviderInitializer(dummyAuthenticationProvider, zosmfAuthenticationProvider, tokenAuthenticationProvider, authProvider);

        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        authProviderInitializer.configure(authenticationManagerBuilder);

        verify(authenticationManagerBuilder).authenticationProvider(tokenAuthenticationProvider);
        verify(authenticationManagerBuilder).authenticationProvider(dummyAuthenticationProvider);
        verify(authenticationManagerBuilder, never()).authenticationProvider(zosmfAuthenticationProvider);
    }

    @Test
    public void testZosmfprovider() {
        String authProvider = LoginProvider.ZOSMF.toString();

        AuthProviderInitializer authProviderInitializer = new AuthProviderInitializer(dummyAuthenticationProvider, zosmfAuthenticationProvider, tokenAuthenticationProvider, authProvider);

        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        authProviderInitializer.configure(authenticationManagerBuilder);

        verify(authenticationManagerBuilder).authenticationProvider(tokenAuthenticationProvider);
        verify(authenticationManagerBuilder).authenticationProvider(zosmfAuthenticationProvider);
        verify(authenticationManagerBuilder, never()).authenticationProvider(dummyAuthenticationProvider);
    }
}
