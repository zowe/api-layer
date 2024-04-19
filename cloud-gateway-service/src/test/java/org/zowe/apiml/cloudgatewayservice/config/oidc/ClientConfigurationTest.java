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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientConfigurationTest {

    private static final String PROVIDER = "oidcprovider";
    private static final String[] SYSTEM_ENVIRONMENTS = {
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_clientId",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_clientSecret",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_redirectUri",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_scope",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_authorizationUri",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_tokenUri",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_userInfoUri",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_userNameAttribute",
        "ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_jwkSetUri"
    };

    @Test
    void givenNoConfiguration_whenCreated_thenReturnNoProvider() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        assertTrue(clientConfiguration.getConfigurations().isEmpty());
        assertFalse(clientConfiguration.isConfigured());
    }

    @Test
    void givenOnlyProvider_whenCreated_thenReturnNoProvider() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        ReflectionTestUtils.setField(clientConfiguration, "provider", Collections.singletonMap("id", new Provider()));
        assertTrue(clientConfiguration.getConfigurations().isEmpty());
        assertFalse(clientConfiguration.isConfigured());
    }

    @Test
    void givenOnlyRegistration_whenCreated_thenReturnNoProvider() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        ReflectionTestUtils.setField(clientConfiguration, "registration", Collections.singletonMap("id", new Registration()));
        assertTrue(clientConfiguration.getConfigurations().isEmpty());
        assertFalse(clientConfiguration.isConfigured());
    }

    @Test
    void givenConfiguration_whenGetConfiguration_thenReturnJustFullProviders() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        Map<String, Registration> registration = (Map<String, Registration>) ReflectionTestUtils.getField(clientConfiguration, "registration");
        Map<String, Provider> provider = (Map<String, Provider>) ReflectionTestUtils.getField(clientConfiguration, "provider");

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
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_clientIdV", registration.getClientId());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_clientSecretV", registration.getClientSecret());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_redirectUriV", registration.getRedirectUri());
        assertEquals(1, registration.getScope().size());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_registration_scopeV", registration.getScope().get(0));
    }

    void assertSystemEnv(Provider provider) {
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_authorizationUriV", provider.getAuthorizationUri());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_tokenUriV", provider.getTokenUri());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_userInfoUriV", provider.getUserInfoUri());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_userNameAttributeV", provider.getUserNameAttribute());
        assertEquals("ZWE_configs_spring_security_oauth2_client_oidcprovider_provider_jwkSetUriV", provider.getJwkSetUri());
    }

    void assertSystemEnv(ClientConfiguration clientConfiguration) {
        assertSystemEnv(clientConfiguration.getProvider().get(PROVIDER));
        assertSystemEnv(clientConfiguration.getRegistration().get(PROVIDER));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void givenSystemEnvironment_whenCreateClientConfiguration_thenSet(boolean providerSet) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        try {
            Arrays.asList(SYSTEM_ENVIRONMENTS).forEach(s -> System.setProperty(s, s + "V"));
            if (providerSet) {
                clientConfiguration.getProvider().put(PROVIDER, new Provider());
                clientConfiguration.getRegistration().put(PROVIDER, new Registration());
            }
            clientConfiguration.updateWithSystemEnvironment();

            assertSystemEnv(clientConfiguration);
        } finally {
            Arrays.asList(SYSTEM_ENVIRONMENTS).forEach(s -> System.getProperties().remove(s));
        }

        // test if missing system environment will be skipped
        clientConfiguration.updateWithSystemEnvironment();
        assertSystemEnv(clientConfiguration);
    }

}