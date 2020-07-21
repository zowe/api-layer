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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.enable.config.EnableApiDiscoveryConfig;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.exception.ServiceDefinitionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@EnableApiDiscovery
@DirtiesContext
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class, classes = {RegisterToApiLayer.class, EnableApiDiscoveryConfig.class})
public class ApimlEnabledRegisterToApiLayerTest {

    @Autowired
    private ApiMediationServiceConfig apiMediationServiceConfig;

    @MockBean
    private ApiMediationClient apiMediationClient;

    @Test
    public void testOnContextRefreshedEventEvent() throws ServiceDefinitionException {
        assertNotNull("ApiMediationServiceConfig is null", apiMediationServiceConfig);
        assertEquals("Service id is not equal", "discoverableclient2", apiMediationServiceConfig.getServiceId());

        assertNotNull("SslConfig is null", apiMediationServiceConfig.getSsl());
        assertEquals("keystore is not equal", "keystore/localhost/localhost.keystore.p12", apiMediationServiceConfig.getSsl().getKeyStore());
        assertEquals("truststore id is not equal", "keystore/localhost/localhost.truststore.p12", apiMediationServiceConfig.getSsl().getTrustStore());

        verify(apiMediationClient, times(1)).register(apiMediationServiceConfig);
    }


}
