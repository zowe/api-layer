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
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.instance.lookup.InstanceLookupExecutor;

import java.util.Collections;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class GatewayInstanceInitializerTest {

    private static final String SERVICE_ID = CoreService.GATEWAY.getServiceId();

    @Autowired
    private GatewayClient gatewayClient;

    @Autowired
    private EurekaClient eurekaClient;

    @Autowired
    private GatewayInstanceInitializer gatewayInstanceInitializer;

    private Application application;

    @BeforeEach
    void setup() {
        application = new Application(
            SERVICE_ID,
            Collections.singletonList(getStandardInstance(SERVICE_ID, "https://localhost:9090/"))
        );
    }

    @Test
    void testInit() {
        assertTimeout(ofMillis(2000), () -> {
            when(
                eurekaClient.getApplication(SERVICE_ID)
            ).thenReturn(application);

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

        @MockBean
        private EurekaClient eurekaClient;

        @Bean
        public GatewayClient gatewayClient() {
            return new GatewayClient(null);
        }

        @Bean
        public InstanceLookupExecutor instanceLookupExecutor() {
            return new InstanceLookupExecutor(
                eurekaClient
            );
        }

        @Bean
        public GatewayInstanceInitializer gatewayInstanceInitializer(ApplicationEventPublisher applicationEventPublisher) {
            return new GatewayInstanceInitializer(
                instanceLookupExecutor(),
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
