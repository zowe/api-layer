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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientConfigurationTest {

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

}