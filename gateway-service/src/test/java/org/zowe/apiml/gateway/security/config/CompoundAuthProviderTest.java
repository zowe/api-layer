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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CompoundAuthProviderTest {

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
        when(environment.getProperty(anyString(), anyString())).thenReturn("dummy");
        compoundAuthProvider = new CompoundAuthProvider(authProvidersMap, environment);
    }

    @Test
    public void testConfiguredLoginAuthProvider() {
        AuthenticationProvider loginAuthProviderName = compoundAuthProvider.getConfiguredLoginAuthProvider();
        assertTrue(loginAuthProviderName instanceof DummyAuthenticationProvider);
    }

    @Test
    public void testGetLoginAuthProviderName() {
        String loginAuthProviderName = compoundAuthProvider.getLoginAuthProviderName();
        assertEquals("dummy", loginAuthProviderName);
    }

    @Test
    public void testSetLoginAuthProvider() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        compoundAuthProvider.setLoginAuthProvider(LoginProvider.SAF.getValue());
        String loginAuthProviderName = compoundAuthProvider.getLoginAuthProviderName();
        assertEquals("saf", loginAuthProviderName);
    }

    @Test
    public void testSetLoginAuthProvider_withoutDevProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        compoundAuthProvider.setLoginAuthProvider(LoginProvider.SAF.getValue());
        String loginAuthProviderName = compoundAuthProvider.getLoginAuthProviderName();
        assertEquals("dummy", loginAuthProviderName);
    }

    @Test
    public void testAuthenticate() {
        compoundAuthProvider.authenticate(any());
        verify(dummyAuthenticationProvider, times(1)).authenticate(any());
    }

    @Test
    public void testSupport() {
        compoundAuthProvider.supports(any());
        verify(dummyAuthenticationProvider, times(1)).supports(any());
    }
}
