/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.config;

import com.netflix.appinfo.EurekaInstanceConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.enable.register.RegisterToApiLayer;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
@EnableApiDiscovery
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class, classes = {RegisterToApiLayer.class, EnableApiDiscoveryConfig.class})
public class EnableApiDiscoveryConfigTest {


    @Autowired
    private ApiMediationServiceConfig apiMediationServiceConfig;

    @MockBean
    private ApiMediationClient apiMediationClient;

    @Test
    public void messageServiceDiscovery() {
        String baseUrl = "localhost";
        String ipAddress = "127.0.0.0";
        String discovery = "https://localhost:10011/discovery";
        String correctMessage = String.format(
            "ZWEA001I Registering to API Mediation Layer: {baseUrl=%s, ipAddress=%s, discoveryServiceUrls=%s}",
            baseUrl, ipAddress, discovery);

        MessageService messageService = new EnableApiDiscoveryConfig().messageServiceDiscovery();
        Message message = messageService.createMessage("org.zowe.apiml.enabler.registration.successful",
            baseUrl, ipAddress, discovery);

        assertTrue(message.mapToLogMessage().contains(correctMessage));
    }


    @Test
    public void resolveServiceIpAddressIfNotProvidedInConfiguration() {
        assertEquals("127.0.0.1", apiMediationServiceConfig.getServiceIpAddress());
    }

    @Test
    public void givenYamlMetadata_whenIpAddressIsPreferred_thenUseIpAddress() throws ServiceDefinitionException {
        EurekaInstanceConfigCreator eurekaInstanceConfigCreator = new EurekaInstanceConfigCreator();
        EurekaInstanceConfig translatedConfig = eurekaInstanceConfigCreator.createEurekaInstanceConfig(apiMediationServiceConfig);
        assertEquals("https://127.0.0.1:10043/discoverableclient2", translatedConfig.getHomePageUrl());
        assertEquals("127.0.0.1", translatedConfig.getHostName(true));
    }

}
