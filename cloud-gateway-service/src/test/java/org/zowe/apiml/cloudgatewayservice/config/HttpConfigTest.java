/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;

import static org.mockito.Mockito.mock;

@SpringBootTest
class HttpConfigTest {

    @Autowired
    private HttpConfig httpConfig;

    @Nested
    class WhenCreateEurekaJerseyClientBuilder {
        @Test
        void thenIsNotNull() {
            Assertions.assertNotNull(httpConfig.getEurekaJerseyClient());
        }
    }

    @Nested
    class WhenCreateRouteLocator {
        @Test
        void thenIsNotNull() {
            ReactiveDiscoveryClient discoveryClient = mock(ReactiveDiscoveryClient.class);
            DiscoveryLocatorProperties properties = mock(DiscoveryLocatorProperties.class);
            Assertions.assertNotNull(httpConfig.discoveryClientRouteDefinitionLocator(discoveryClient, properties));
        }
    }

    @Nested
    class WhenInitializeEurekaClient {
        @Mock
        private ApplicationInfoManager manager;

        @Mock
        private EurekaClientConfig config;

        @Mock
        private EurekaJerseyClient eurekaJerseyClient;

        @Mock
        private EurekaInstanceConfig instance;

        @Mock
        private HealthCheckHandler healthCheckHandler;

        @Test
        void thenCreateIt() {
            Assertions.assertNotNull(httpConfig.eurekaClient(manager, config, eurekaJerseyClient, instance, healthCheckHandler));
        }
    }

}

