/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VerificationOnboardServiceTest {

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;

    private static final String GATEWAY_ID = "gateway";
    private static final String SWAGGER_NAME = "apiml.apiInfo.api-v2.swaggerUrl";
    private static final String SWAGGER_URL = "https://hostname/sampleclient/api-doc";

    @Test
    void whenCheckingOnboardedService() {
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));
        assertFalse(verificationOnboardService.checkOnboarding("Test"));
        assertTrue(verificationOnboardService.checkOnboarding("OnboardedService"));

    }


    @ParameterizedTest
    @MethodSource("provideGatewayConfiguration")
    void givenGatewayConfiguration_thenReturnSwagger(Map<String, String> metadata, String expectedUrl, String serviceId, boolean expectServiceInstance) {
        DefaultServiceInstance defaultServiceInstance = new DefaultServiceInstance("sys1.acme.net", serviceId, "localhost", 10010, true, metadata);
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        if (expectServiceInstance) {
            serviceInstances.add(defaultServiceInstance);
        }
        when(discoveryClient.getInstances(GATEWAY_ID)).thenReturn(serviceInstances);

        String actualUrl = verificationOnboardService.retrieveSwagger(GATEWAY_ID);
        assertEquals(expectedUrl, actualUrl);
    }

    private static Stream<Arguments> provideGatewayConfiguration() {
        return Stream.of(
            Arguments.of(Collections.singletonMap(SWAGGER_NAME, SWAGGER_URL), SWAGGER_URL, GATEWAY_ID, true),
            Arguments.of(Collections.singletonMap(SWAGGER_NAME, SWAGGER_URL), "", GATEWAY_ID, false),
            Arguments.of(Collections.emptyMap(), "", GATEWAY_ID, true),
            Arguments.of(Collections.singletonMap("randomName", "randomValue"), "", GATEWAY_ID, true)
        );
    }


}

