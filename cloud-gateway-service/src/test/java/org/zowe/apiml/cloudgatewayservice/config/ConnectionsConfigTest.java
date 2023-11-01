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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ComponentScan(basePackages = "org.zowe.apiml.cloudgatewayservice")
class ConnectionsConfigTest {

    @Autowired
    private ConnectionsConfig connectionsConfig;
    @Autowired
    private RoutingConfig routingConfig;

    @Nested
    class WhenCreateEurekaJerseyClientBuilder {
        @Test
        void thenIsNotNull() {
            Assertions.assertNotNull(connectionsConfig);
            Assertions.assertNotNull(connectionsConfig.getEurekaJerseyClient());
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
            Assertions.assertNotNull(connectionsConfig.primaryEurekaClient(manager, config, eurekaJerseyClient, healthCheckHandler));
        }
    }

    @Nested
    class KeyringFormatAndPasswordUpdate {

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            ConnectionsConfig connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "keyStorePath", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(connectionsConfig, "trustStorePath", "safkeyring:////userId/ringId2");

            connectionsConfig.updateConfigParameters();

            assertEquals("safkeyring://userId/ringId1", ReflectionTestUtils.getField(connectionsConfig, "keyStorePath"));
            assertEquals("safkeyring://userId/ringId2", ReflectionTestUtils.getField(connectionsConfig, "trustStorePath"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(connectionsConfig, "keyStorePassword"));
            assertArrayEquals("password".toCharArray(), (char[]) ReflectionTestUtils.getField(connectionsConfig, "trustStorePassword"));
        }

        @Test
        void whenKeystore_thenDoNothing() {
            ConnectionsConfig connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "keyStorePath", "/path1");
            ReflectionTestUtils.setField(connectionsConfig, "trustStorePath", "/path2");

            connectionsConfig.updateConfigParameters();

            assertEquals("/path1", ReflectionTestUtils.getField(connectionsConfig, "keyStorePath"));
            assertEquals("/path2", ReflectionTestUtils.getField(connectionsConfig, "trustStorePath"));
            assertNull(ReflectionTestUtils.getField(connectionsConfig, "keyStorePassword"));
            assertNull(ReflectionTestUtils.getField(connectionsConfig, "trustStorePassword"));
        }

    }

}

