/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.gateway;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.instance.lookup.InstanceLookupExecutor;

import java.util.List;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class GatewayInstanceInitializerTest {

    private static final String SERVICE_ID = CoreService.GATEWAY.getServiceId();

    @Autowired
    private GatewayClient gatewayClient;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private GatewayInstanceInitializer gatewayInstanceInitializer;

    @Test
    void testInit() {
        assertTimeout(ofMillis(2000), () -> {
            when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_ID));
            when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(List.of(new EurekaServiceInstance(getStandardInstance(SERVICE_ID, "https://localhost:9090/"))));

            gatewayInstanceInitializer.init();

            while (!gatewayClient.isInitialized()) ;
        });

        ServiceAddress gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        assertNotNull(gatewayConfigProperties);
        assertEquals("https", gatewayConfigProperties.getScheme());
        assertEquals("localhost:9090", gatewayConfigProperties.getHostname());
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        DiscoveryClient discoveryClient() {
            return mock(DiscoveryClient.class);
        }

        @Bean
        GatewayClient gatewayClient() {
            return new GatewayClient(null);
        }

        @Bean
        InstanceLookupExecutor instanceLookupExecutor(DiscoveryClient discoveryClient) {
            return new InstanceLookupExecutor(
                discoveryClient
            );
        }

        @Bean
        GatewayInstanceInitializer gatewayInstanceInitializer(ApplicationEventPublisher applicationEventPublisher, DiscoveryClient discoveryClient) {
            return new GatewayInstanceInitializer(
                instanceLookupExecutor(discoveryClient),
                applicationEventPublisher,
                gatewayClient()
            );
        }

    }

    private InstanceInfo getStandardInstance(String serviceId, String homePageUrl) {
        return InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setHostName("localhost")
            .setHomePageUrl(homePageUrl, homePageUrl)
            .build();
    }

}
