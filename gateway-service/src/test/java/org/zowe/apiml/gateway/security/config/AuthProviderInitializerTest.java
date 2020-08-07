/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.gateway.security.login.dummy.DummyAuthenticationProvider;
import org.zowe.apiml.gateway.security.login.saf.ZosAuthenticationProvider;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.gateway.security.query.TokenAuthenticationProvider;

import static org.mockito.Mockito.*;

class AuthProviderInitializerTest {

    private DummyAuthenticationProvider dummyAuthenticationProvider;
    private TokenAuthenticationProvider tokenAuthenticationProvider;
    private ZosmfAuthenticationProvider zosmfAuthenticationProvider;
    private CertificateAuthenticationProvider certificateAuthenticationProvider;
    private ZosAuthenticationProvider zosAuthenticationProvider;

    @BeforeEach
    void setup() {
        dummyAuthenticationProvider = mock(DummyAuthenticationProvider.class);
        tokenAuthenticationProvider = mock(TokenAuthenticationProvider.class);
        zosmfAuthenticationProvider = mock(ZosmfAuthenticationProvider.class);
        certificateAuthenticationProvider = mock(CertificateAuthenticationProvider.class);
        zosAuthenticationProvider = mock(ZosAuthenticationProvider.class);
    }

    @Test
    void testConfigure_whenProviderIsDummy() {
        String authProvider = LoginProvider.DUMMY.toString();

        AuthProviderInitializer authProviderInitializer = new AuthProviderInitializer(
            dummyAuthenticationProvider, zosmfAuthenticationProvider, tokenAuthenticationProvider,
            certificateAuthenticationProvider, zosAuthenticationProvider, authProvider
        );

        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        authProviderInitializer.configure(authenticationManagerBuilder);

        verify(authenticationManagerBuilder).authenticationProvider(tokenAuthenticationProvider);
        verify(authenticationManagerBuilder).authenticationProvider(dummyAuthenticationProvider);
        verify(authenticationManagerBuilder, never()).authenticationProvider(zosmfAuthenticationProvider);
    }

    @Test
    void testConfigure_whenProviderIsZOSMF() {
        String authProvider = LoginProvider.ZOSMF.toString();

        AuthProviderInitializer authProviderInitializer = new AuthProviderInitializer(
            dummyAuthenticationProvider, zosmfAuthenticationProvider, tokenAuthenticationProvider,
            certificateAuthenticationProvider, zosAuthenticationProvider, authProvider
        );

        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        authProviderInitializer.configure(authenticationManagerBuilder);

        verify(authenticationManagerBuilder).authenticationProvider(tokenAuthenticationProvider);
        verify(authenticationManagerBuilder).authenticationProvider(zosmfAuthenticationProvider);
        verify(authenticationManagerBuilder, never()).authenticationProvider(dummyAuthenticationProvider);
    }

    @Test
    void testConfigure_whenProviderIsUnexpectedString() {
        String authProvider = "unexpectedProvider";

        AuthProviderInitializer authProviderInitializer = new AuthProviderInitializer(
            dummyAuthenticationProvider, zosmfAuthenticationProvider, tokenAuthenticationProvider,
            certificateAuthenticationProvider, zosAuthenticationProvider, authProvider
        );

        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        authProviderInitializer.configure(authenticationManagerBuilder);

        verify(authenticationManagerBuilder).authenticationProvider(tokenAuthenticationProvider);
        verify(authenticationManagerBuilder).authenticationProvider(zosmfAuthenticationProvider);
        verify(authenticationManagerBuilder, never()).authenticationProvider(dummyAuthenticationProvider);
    }
}
