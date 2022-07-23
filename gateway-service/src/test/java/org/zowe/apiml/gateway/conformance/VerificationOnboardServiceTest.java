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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@ExtendWith(MockitoExtension.class)
public class VerificationOnboardServiceTest {

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;
    @Nested
    class GivenRegisteredService {

        @Test
        void whenServiceId_Registered() {

            List<String> serviceList = new ArrayList<String>();
            serviceList.add("gateway");
            when(discoveryClient.getServices()).thenReturn(serviceList);

            Boolean registeredInfo = verificationOnboardService.checkOnboarding("gateway");
            Boolean expectedInfo = serviceList.contains("gateway");

            assertEquals(expectedInfo, registeredInfo);
        }


        @Test
        void whenServiceId_Registered_RetrieveData() throws ClientProtocolException, java.io.IOException {
            
            Map<String, String> metadata = Collections.singletonMap("apiml.apiInfo.api-v2.swaggerUrl", "https://hostname/sampleclient/api-doc");
            DefaultServiceInstance defaultServiceInstance = new DefaultServiceInstance("sys1.acme.net", "gateway", "localhost", 10010, true, metadata);
            List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
            serviceInstances.add(defaultServiceInstance);

            when(discoveryClient.getInstances(any())).thenReturn(serviceInstances);
            
            String expectedUrl = serviceInstances.get(0).getMetadata().get("apiml.apiInfo.api-v2.swaggerUrl");
            String actualUrl = verificationOnboardService.retrieveMetaData("gateway");
            assertEquals(expectedUrl, actualUrl);
        }

    }

    @Nested
    class GivenInvalidService {
        
        @Test
        void whenServiceId_Unregistered() {
            List<String> serviceList = new ArrayList<String>();
            serviceList.add("zowesample");
            when(discoveryClient.getServices()).thenReturn(serviceList);

            Boolean registeredInfo = verificationOnboardService.checkOnboarding("gateway");
            Boolean expectedInfo = serviceList.contains("gateway");

            assertEquals(expectedInfo, registeredInfo);
        }

        @Test
        void whenServiceId_Registered_CannotRetrieveData() throws ClientProtocolException, java.io.IOException {
            
            Map<String, String> metadata = Collections.singletonMap("randomInvalidName", "InvalidUrl");
            DefaultServiceInstance defaultServiceInstance = new DefaultServiceInstance("sys1.acme.net", "gateway", "localhost", 10010, true, metadata);
            List<ServiceInstance> serviceInstances = new ArrayList<ServiceInstance>();
            serviceInstances.add(defaultServiceInstance);

            when(discoveryClient.getInstances(any())).thenReturn(serviceInstances);
            
            String expectedUrl = "";
            String actualUrl = verificationOnboardService.retrieveMetaData("gateway");
            assertEquals(expectedUrl, actualUrl);
        }
    }
}

