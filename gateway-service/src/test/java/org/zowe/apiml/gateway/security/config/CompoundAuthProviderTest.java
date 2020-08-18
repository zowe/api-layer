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
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.gateway.security.login.dummy.DummyAuthenticationProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


class CompoundAuthProviderTest {

    private Map<String, AuthenticationProvider> authProvidersMap;
    private AuthenticationProvider dummyAuthenticationProvider;
    private Environment environment;
    private CompoundAuthProvider compoundAuthProvider;

    @BeforeEach
    void setup() {
        dummyAuthenticationProvider = mock(DummyAuthenticationProvider.class);
        authProvidersMap = new HashMap<>();
        authProvidersMap.put(LoginProvider.DUMMY.getAuthProviderBeanName(), dummyAuthenticationProvider);
        environment = mock(Environment.class);
        compoundAuthProvider = new CompoundAuthProvider(authProvidersMap, environment, "dummy");
    }

    @Test
    void testGetLoginAuthProviderName() {
        String loginAuthProviderName = compoundAuthProvider.getLoginAuthProviderName();
        assertEquals("dummy", loginAuthProviderName);
    }

    @Test
    void testSetLoginAuthProvider() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"diag"});
        compoundAuthProvider.setLoginAuthProvider(LoginProvider.SAF.getValue());
        String loginAuthProviderName = compoundAuthProvider.getLoginAuthProviderName();
        assertEquals("saf", loginAuthProviderName);
    }

    @Test
    void testSetLoginAuthProvider_withoutDevProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        compoundAuthProvider.setLoginAuthProvider(LoginProvider.SAF.getValue());
        String loginAuthProviderName = compoundAuthProvider.getLoginAuthProviderName();
        assertEquals("dummy", loginAuthProviderName);
    }

    @Test
    void testAuthenticate() {
        compoundAuthProvider.authenticate(any());
        verify(dummyAuthenticationProvider, times(1)).authenticate(any());
    }

    @Test
    void testSupport() {
        compoundAuthProvider.supports(any());
        verify(dummyAuthenticationProvider, times(1)).supports(any());
    }
}
