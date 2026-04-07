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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.enable.register.RegisterToApiLayer;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.eurekaservice.client.util.EurekaInstanceConfigCreator;
import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration
@EnableApiDiscovery
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = {RegisterToApiLayer.class, EnableApiDiscoveryConfig.class})
class EnableApiDiscoveryConfigTest {

    @Autowired
    private ApiMediationServiceConfig apiMediationServiceConfig;

    @MockitoBean
    private ApiMediationClient apiMediationClient;

    @Test
    void messageServiceDiscovery() {
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
    void resolveServiceIpAddressIfNotProvidedInConfiguration() {
        assertEquals("127.0.0.1", apiMediationServiceConfig.getServiceIpAddress());
    }

    @Test
    void givenYamlMetadata_whenIpAddressIsPreferred_thenUseIpAddress() throws ServiceDefinitionException {
        EurekaInstanceConfigCreator eurekaInstanceConfigCreator = new EurekaInstanceConfigCreator();
        EurekaInstanceConfig translatedConfig = eurekaInstanceConfigCreator.createEurekaInstanceConfig(apiMediationServiceConfig);
        assertEquals("https://127.0.0.1:10043/discoverableclient2", translatedConfig.getHomePageUrl());
        assertEquals("127.0.0.1", translatedConfig.getHostName(true));
    }

    @ParameterizedTest(name = "Call apiMediationClient with EurekaClientProvider: {0} and EurekaClientConfigProvider: {1}")
    @MethodSource("provideTestData")
    void apiMediationClient(EurekaClientProvider clientProvider, EurekaClientConfigProvider clientConfigProvider, boolean isClientProviderExpectedSame, boolean isClientProviderConfigExpectedSame) {
        ObjectProvider<EurekaClientProvider> objectProviderForClientProvider = mock(ObjectProvider.class);
        ObjectProvider<EurekaClientConfigProvider> objectProviderForClientConfigProvider = mock(ObjectProvider.class);

        when(objectProviderForClientProvider.getIfAvailable()).thenReturn(clientProvider);
        when(objectProviderForClientConfigProvider.getIfAvailable()).thenReturn(clientConfigProvider);

        ApiMediationClientImpl actualApiMediationClientImpl = (ApiMediationClientImpl) new EnableApiDiscoveryConfig().apiMediationClient(objectProviderForClientProvider, objectProviderForClientConfigProvider);
        EurekaClientProvider actualClientProvider = (EurekaClientProvider) ReflectionTestUtils.getField(actualApiMediationClientImpl, "eurekaClientProvider");
        EurekaClientConfigProvider actualClientConfigProvider = (EurekaClientConfigProvider) ReflectionTestUtils.getField(actualApiMediationClientImpl, "eurekaClientConfigProvider");

        assertExpectedValue(clientProvider, actualClientProvider, isClientProviderExpectedSame);
        assertExpectedValue(clientConfigProvider, actualClientConfigProvider, isClientProviderConfigExpectedSame);
    }

    private void assertExpectedValue(Object expectedValue, Object actualValue, boolean isExpectedToBeSame) {
        if (isExpectedToBeSame) {
            assertEquals(expectedValue, actualValue);
        } else {
            assertNotNull(actualValue);
        }
    }

    static Stream<Arguments> provideTestData() {
        EurekaClientProvider eurekaClientProvider = mock(EurekaClientProvider.class);
        EurekaClientConfigProvider eurekaClientConfigProvider = mock(EurekaClientConfigProvider.class);

        return Stream.of(
            Arguments.of(null, null, false, false),
            Arguments.of(null, eurekaClientConfigProvider, false, false),
            Arguments.of(eurekaClientProvider, null, true, false),
            Arguments.of(eurekaClientProvider, eurekaClientConfigProvider, true, true));
    }

}
