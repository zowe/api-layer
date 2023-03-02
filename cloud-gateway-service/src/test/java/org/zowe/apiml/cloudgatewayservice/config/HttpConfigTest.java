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
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ComponentScan(basePackages = "org.zowe.apiml.cloudgatewayservice")
class HttpConfigTest {

    @Autowired
    private HttpConfig httpConfig;

    @Nested
    class WhenCreateEurekaJerseyClientBuilder {
        @Test
        void thenIsNotNull() {
            Assertions.assertNotNull(httpConfig);
            Assertions.assertNotNull(httpConfig.getEurekaJerseyClient());
        }
    }

    @Nested
    class WhenCreateRouteLocator {
        @Test
        void thenIsNotNull() {
            ReactiveDiscoveryClient discoveryClient = mock(ReactiveDiscoveryClient.class);
            DiscoveryLocatorProperties properties = mock(DiscoveryLocatorProperties.class);
            Assertions.assertNotNull(httpConfig.proxyRouteDefLocator(discoveryClient, properties, Collections.singletonList(new FilterDefinition("name=value")), null, null));
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
        private HealthCheckHandler healthCheckHandler;

        @Test
        void thenCreateIt() {
            Assertions.assertNotNull(httpConfig.eurekaClient(manager, config, eurekaJerseyClient, healthCheckHandler));
        }
    }

    @Nested
    class KeyringFormatAndPasswordUpdate {

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            HttpConfig httpConfig = new HttpConfig(null);
            ReflectionTestUtils.setField(httpConfig, "keyStorePath", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(httpConfig, "trustStorePath", "safkeyring:////userId/ringId2");

            httpConfig.init();

            assertEquals("safkeyring://userId/ringId1", ReflectionTestUtils.getField(httpConfig, "keyStorePath"));
            assertEquals("safkeyring://userId/ringId2", ReflectionTestUtils.getField(httpConfig, "trustStorePath"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(httpConfig, "keyStorePassword"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(httpConfig, "trustStorePassword"));
        }

        @Test
        void whenKeystore_thenDoNothing() {
            HttpConfig httpConfig = new HttpConfig(null);
            ReflectionTestUtils.setField(httpConfig, "keyStorePath", "/path1");
            ReflectionTestUtils.setField(httpConfig, "trustStorePath", "/path2");

            httpConfig.init();

            assertEquals("/path1", ReflectionTestUtils.getField(httpConfig, "keyStorePath"));
            assertEquals("/path2", ReflectionTestUtils.getField(httpConfig, "trustStorePath"));
            assertNull(ReflectionTestUtils.getField(httpConfig, "keyStorePassword"));
            assertNull(ReflectionTestUtils.getField(httpConfig, "trustStorePassword"));
        }

    }

}

