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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.zowe.apiml.client.model.DiscoverableClientConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;

import static junit.framework.TestCase.assertFalse;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@Import(ApiMediationClientServiceTest.TestConfig.class)
public class ApiMediationClientServiceTest {

    private ApiMediationClientService apiMediationClientService;

    @Autowired
    private DiscoverableClientConfig discoverableClientConfig;

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        apiMediationClientService = new ApiMediationClientService(discoverableClientConfig);
    }

    @Test
    public void registerTest() throws ServiceDefinitionException {
        apiMediationClientService.register();
    }

    @Test
    public void registerTest_duplicate() throws ServiceDefinitionException {
        exceptionRule.expect(ServiceDefinitionException.class);
        apiMediationClientService.register();
        apiMediationClientService.register();
    }

    @Test
    public void isRegisteredTest_notRegistered() {
        assertFalse(apiMediationClientService.isRegistered());
    }

    @Test
    public void unregisterTest() {
        apiMediationClientService.unregister();
    }

    @Test
    public void unregisterTest_notRegistered() {
        apiMediationClientService.unregister();
    }

    @Configuration
    public static class TestConfig {
        @Bean
        public DiscoverableClientConfig discoverableClientConfig() {
            return new DiscoverableClientConfig();
        }
    }
}
