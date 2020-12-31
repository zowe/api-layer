/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.client.model.DiscoverableClientConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@Import(ApiMediationClientServiceTest.TestConfig.class)
class ApiMediationClientServiceTest {

    private ApiMediationClientService apiMediationClientService;

    @Autowired
    private DiscoverableClientConfig discoverableClientConfig;

    @BeforeEach
    void setup() {
        apiMediationClientService = new ApiMediationClientService(discoverableClientConfig);
    }

    @Test
    void registerTest() throws ServiceDefinitionException {
        assertTrue(apiMediationClientService.register());
    }

    @Test
    void registerTest_duplicate() throws ServiceDefinitionException {
        assertTrue(apiMediationClientService.register());
        assertThrows(ServiceDefinitionException.class, () -> apiMediationClientService.register());
    }

    @Test
    void isRegisteredTest_notRegistered() {
        assertFalse(apiMediationClientService.isRegistered());
    }

    @Test
    void unregisterTest() {
        assertTrue(apiMediationClientService.unregister());
    }

    @Test
    void unregisterTest_notRegistered() {
        assertTrue(apiMediationClientService.unregister());
    }

    @Configuration
    public static class TestConfig {
        @Bean
        public DiscoverableClientConfig discoverableClientConfig() {
            return new DiscoverableClientConfig();
        }
    }
}
