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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.enable.config.EnableApiDiscoveryConfig;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@EnableApiDiscovery
@DirtiesContext
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class, classes = {RegisterToApiLayer.class, EnableApiDiscoveryConfig.class})
class ApimlEnabledRegisterToApiLayerTest {

    @Autowired
    private ApiMediationServiceConfig apiMediationServiceConfig;

    @MockBean
    private ApiMediationClient apiMediationClient;

    @Test
    void testOnContextRefreshedEventEvent() throws ServiceDefinitionException {
        assertNotNull(apiMediationServiceConfig, "ApiMediationServiceConfig is null");
        assertEquals("discoverableclient2", apiMediationServiceConfig.getServiceId(), "Service id is not equal");

        assertNotNull(apiMediationServiceConfig.getSsl(), "SslConfig is null");
        assertEquals("keystore/localhost/localhost.keystore.p12", apiMediationServiceConfig.getSsl().getKeyStore(), "keystore is not equal");
        assertEquals("keystore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore(), "truststore id is not equal");

        verify(apiMediationClient, times(1)).register(apiMediationServiceConfig);
    }

}
