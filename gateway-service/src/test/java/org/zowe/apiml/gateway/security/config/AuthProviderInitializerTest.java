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
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.zowe.apiml.gateway.security.query.TokenAuthenticationProvider;

import static org.mockito.Mockito.*;

class AuthProviderInitializerTest {

    private CompoundAuthProvider loginAuthProvider;
    private TokenAuthenticationProvider tokenAuthenticationProvider;

    @BeforeEach
    void setup() {
        tokenAuthenticationProvider = mock(TokenAuthenticationProvider.class);
        loginAuthProvider =  mock(CompoundAuthProvider.class);
    }

    @Test
    void givenValidProviders_whenTheClassIsCreated_thenTheProvidersArePassedToSpring() {
        AuthProviderInitializer authProviderInitializer = new AuthProviderInitializer(
            loginAuthProvider,
            tokenAuthenticationProvider
        );

        AuthenticationManagerBuilder authenticationManagerBuilder = mock(AuthenticationManagerBuilder.class);
        authProviderInitializer.configure(authenticationManagerBuilder);

        verify(authenticationManagerBuilder).authenticationProvider(loginAuthProvider);
        verify(authenticationManagerBuilder).authenticationProvider(tokenAuthenticationProvider);
        verify(authenticationManagerBuilder).authenticationProvider(any(PreAuthenticatedAuthenticationProvider.class));
    }
}
