/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config.oidc;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ClientConfigurationTest {

    private static final String PROVIDER = "oidcprovider";
    private static final String[] SYSTEM_ENVIRONMENTS = {
        "ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_clientId",
        "ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_clientSecret",
        "ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_redirectUri",
        "ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_scope",
        "ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_authorizationUri",
        "ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_tokenUri",
        "ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_userInfoUri",
        "ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_userNameAttribute",
        "ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_jwkSetUri"
    };

    @Nested
    class WhenCreatingConfiguration {

        @Test
        void givenNoConfiguration_thenReturnNoProvider() {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            assertTrue(clientConfiguration.getConfigurations().isEmpty());
            assertFalse(clientConfiguration.isConfigured());
        }

        @Test
        void givenOnlyProvider_thenReturnNoProvider() {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            ReflectionTestUtils.setField(clientConfiguration, "provider", Collections.singletonMap("id", new Provider()));
            assertTrue(clientConfiguration.getConfigurations().isEmpty());
            assertFalse(clientConfiguration.isConfigured());
        }

        @Test
        void givenOnlyRegistration_thenReturnNoProvider() {
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            ReflectionTestUtils.setField(clientConfiguration, "registration", Collections.singletonMap("id", new Registration()));
            assertTrue(clientConfiguration.getConfigurations().isEmpty());
            assertFalse(clientConfiguration.isConfigured());
        }
    }

    @Test
    void givenConfiguration_whenGetConfiguration_thenReturnJustFullProviders() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        Map<String, Registration> registration = clientConfiguration.getRegistration();
        Map<String, Provider> provider = clientConfiguration.getProvider();

        registration.put("id1", new Registration());
        registration.put("id2", new Registration());
        registration.put("id3", new Registration());

        provider.put("id2", new Provider());
        provider.put("id3", new Provider());
        provider.put("id4", new Provider());

        Map<String, ClientConfiguration.Config> configMap = clientConfiguration.getConfigurations();
        assertTrue(clientConfiguration.isConfigured());
        assertEquals(2, configMap.size());
        assertSame(registration.get("id2"), configMap.get("id2").getRegistration());
        assertSame(provider.get("id2"), configMap.get("id2").getProvider());
        assertSame(registration.get("id3"), configMap.get("id3").getRegistration());
        assertSame(provider.get("id3"), configMap.get("id3").getProvider());
    }

    void assertSystemEnv(Registration registration) {
        assertEquals("ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_clientIdV", registration.getClientId());
        assertEquals("ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_clientSecretV", registration.getClientSecret());
        assertEquals("ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_redirectUriV", registration.getRedirectUri());
        assertEquals(1, registration.getScope().size());
        assertEquals("ZWE_configs_spring_security_oauth2_client_registration_oidcprovider_scopeV", registration.getScope().get(0));
    }

    void assertSystemEnv(Provider provider) {
        assertEquals("ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_authorizationUriV", provider.getAuthorizationUri());
        assertEquals("ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_tokenUriV", provider.getTokenUri());
        assertEquals("ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_userInfoUriV", provider.getUserInfoUri());
        assertEquals("ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_userNameAttributeV", provider.getUserNameAttribute());
        assertEquals("ZWE_configs_spring_security_oauth2_client_provider_oidcprovider_jwkSetUriV", provider.getJwkSetUri());
    }

    void assertSystemEnv(ClientConfiguration clientConfiguration) {
        assertSystemEnv(clientConfiguration.getProvider().get(PROVIDER));
        assertSystemEnv(clientConfiguration.getRegistration().get(PROVIDER));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void givenSystemEnvironment_whenCreateClientConfiguration_thenSet(boolean providerSet) throws NoSuchFieldException, IllegalAccessException {
        assumeFalse(StringUtils.containsIgnoreCase(System.getProperty("os.name"), "win"));

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        Class<?> envVarClass = System.getenv().getClass();
        Field mField = envVarClass.getDeclaredField("m");
        mField.setAccessible(true);
        Map<String, String> writeableEnvVars = (Map<String, String>) mField.get(System.getenv());
        try {
            Arrays.asList(SYSTEM_ENVIRONMENTS).forEach(s -> writeableEnvVars.put(s, s + "V"));
            if (providerSet) {
                clientConfiguration.getProvider().put(PROVIDER, new Provider());
                clientConfiguration.getRegistration().put(PROVIDER, new Registration());
            }
            clientConfiguration.updateWithSystemEnvironment();

            assertSystemEnv(clientConfiguration);
        } finally {
            Arrays.asList(SYSTEM_ENVIRONMENTS).forEach(s -> writeableEnvVars.remove(s));
        }

        // test if missing system environment will be skipped
        clientConfiguration.updateWithSystemEnvironment();
        assertSystemEnv(clientConfiguration);
    }

}
