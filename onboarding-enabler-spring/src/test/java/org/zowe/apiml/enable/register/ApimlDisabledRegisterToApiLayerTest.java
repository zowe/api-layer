/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.register;

import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.enable.config.EnableApiDiscoveryConfig;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@EnableApiDiscovery
@ActiveProfiles("apiml-disabled")
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class, classes = {RegisterToApiLayer.class, EnableApiDiscoveryConfig.class})
public class ApimlDisabledRegisterToApiLayerTest {

    @Autowired
    private ApiMediationServiceConfig apiMediationServiceConfig;

    @MockBean
    private ApiMediationClient apiMediationClient;

    @Test
    public void testOnContextRefreshedEventEvent() throws ServiceDefinitionException {

        assertNotNull("ApiMediationServiceConfig is null", apiMediationServiceConfig);
        assertNotNull("Ssl is null", apiMediationServiceConfig.getSsl());

        verify(apiMediationClient, never()).register(apiMediationServiceConfig);
    }
}
