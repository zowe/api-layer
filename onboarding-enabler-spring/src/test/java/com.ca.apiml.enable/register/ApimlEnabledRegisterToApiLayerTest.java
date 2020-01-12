/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.enable.register;

import com.ca.apiml.enable.config.EnableApiDiscoveryConfig;
import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.exception.ServiceDefinitionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class, classes = {RegisterToApiLayer.class, EnableApiDiscoveryConfig.class})
public class ApimlEnabledRegisterToApiLayerTest {

    @Autowired
    private RegisterToApiLayer registerToApiLayer;

    @Autowired
    private ApiMediationServiceConfig apiMediationServiceConfig;

    @Autowired
    private Ssl ssl;

    @MockBean
    private ApiMediationClient apiMediationClient;

    @Test
    public void testOnContextRefreshedEventEvent() throws ServiceDefinitionException {
        assertNotNull("ApiMediationServiceConfig is null", apiMediationServiceConfig);
        assertEquals("Service id is not equal", "discoverableclient2", apiMediationServiceConfig.getServiceId());

        assertNotNull("SslConfig is null", ssl);
        assertEquals("keystore is not equal", "keystore/localhost/localhost.keystore.p12", ssl.getKeyStore());
        assertEquals("truststore id is not equal", "keystore/localhost/localhost.truststore.p12", ssl.getTrustStore());
    }
}
