/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionsConfigTest {

    @Nested
    @SpringBootTest
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    class WhenCreateEurekaJerseyClientBuilder {

        @Autowired
        private ConnectionsConfig connectionsConfig;

        @Test
        void thenIsNotNull() {
            assertThat(connectionsConfig).isNotNull();
        }

    }

    @Nested
    @SpringBootTest
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    class WhenInitializeEurekaClient {

        @Autowired
        private ConnectionsConfig connectionsConfig;

        @Mock
        private ApplicationInfoManager manager;

        @Mock
        private EurekaClientConfig config;

        @Mock
        private HealthCheckHandler healthCheckHandler;

        @Test
        void thenCreateIt() {
            assertThat(connectionsConfig.primaryEurekaClient(manager, config, healthCheckHandler)).isNotNull();
        }

    }

    @Nested
    @SpringBootTest
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    class KeyringFormatAndPasswordUpdate {

        ApplicationContext context;

        ConnectionsConfig noContextConnectionsConfig = new ConnectionsConfig(null);

        @BeforeEach
        void setup() {
            context = mock(ApplicationContext.class);
            ServerProperties properties = new ServerProperties();
            properties.setSsl(new Ssl());
            when(context.getBean(ServerProperties.class)).thenReturn(properties);
        }

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            ReflectionTestUtils.setField(noContextConnectionsConfig, "keyStorePath", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(noContextConnectionsConfig, "trustStorePath", "safkeyring:////userId/ringId2");
            ReflectionTestUtils.setField(noContextConnectionsConfig, "context", context);
            noContextConnectionsConfig.updateConfigParameters();

            assertThat(ReflectionTestUtils.getField(noContextConnectionsConfig, "keyStorePath")).isEqualTo("safkeyring://userId/ringId1");
            assertThat(ReflectionTestUtils.getField(noContextConnectionsConfig, "trustStorePath")).isEqualTo("safkeyring://userId/ringId2");
            assertThat((char[]) ReflectionTestUtils.getField(noContextConnectionsConfig, "keyStorePassword")).isEqualTo("password".toCharArray());
            assertThat((char[]) ReflectionTestUtils.getField(noContextConnectionsConfig, "trustStorePassword")).isEqualTo("password".toCharArray());
        }

        @Test
        void whenKeystore_thenDoNothing() {
            ReflectionTestUtils.setField(noContextConnectionsConfig, "keyStorePath", "/path1");
            ReflectionTestUtils.setField(noContextConnectionsConfig, "trustStorePath", "/path2");
            ReflectionTestUtils.setField(noContextConnectionsConfig, "context", context);
            noContextConnectionsConfig.updateConfigParameters();

            assertThat(ReflectionTestUtils.getField(noContextConnectionsConfig, "keyStorePath")).isEqualTo("/path1");
            assertThat(ReflectionTestUtils.getField(noContextConnectionsConfig, "trustStorePath")).isEqualTo("/path2");
            assertThat(ReflectionTestUtils.getField(noContextConnectionsConfig, "keyStorePassword")).isNull();
            assertThat(ReflectionTestUtils.getField(noContextConnectionsConfig, "trustStorePassword")).isNull();
        }

    }

    @Nested
    class AdditionalRegistration {

        private EurekaInstanceConfig createConfig() {
            var config = mock(EurekaInstanceConfig.class);
            doReturn(30).when(config).getLeaseRenewalIntervalInSeconds();
            doReturn(90).when(config).getLeaseExpirationDurationInSeconds();
            doReturn("namespace").when(config).getNamespace();
            doReturn("serviceId").when(config).getAppname();
            doReturn("instanceId").when(config).getInstanceId();
            doReturn("/").when(config).getHomePageUrlPath();
            doReturn("/application/health").when(config).getHealthCheckUrlPath();
            doReturn("/application/status").when(config).getStatusPageUrlPath();
            doReturn("https://localhost:10010/").when(config).getHomePageUrl();
            doReturn(10010).when(config).getSecurePort();
            doReturn(true).when(config).getSecurePortEnabled();

            return config;
        }

        @Test
        void givenInvalidUrl_whenCreate_thenThrowAnException() {
            var connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "externalUrl", "invalidUrl");
            var e = assertThrows(RuntimeException.class, () -> connectionsConfig.create(createConfig()));
            assertInstanceOf(MalformedURLException.class, e.getCause());
        }

        @Test
        void givenValidInputs_whenCreate_thenCreateIt() {
            var config = createConfig();
            var connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "externalUrl", "https://domain:1234/");

            InstanceInfo instanceInfo = connectionsConfig.create(config);

            assertNotNull(instanceInfo);
            assertEquals("https://domain:1234/", instanceInfo.getHomePageUrl());
            assertEquals("https://domain:1234/application/health", instanceInfo.getSecureHealthCheckUrl());
            assertEquals("https://domain:1234/application/status", instanceInfo.getStatusPageUrl());
        }

        @Test
        void givenMetadataWithUrl_whenCreate_thenUpdateThem() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("swaggerUrl", "https://localhost:10010/swagger");
            metadata.put("otherKey", "otherValue");

            var config = createConfig();
            doReturn(metadata).when(config).getMetadataMap();

            var connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "externalUrl", "https://domain:1234/");

            InstanceInfo instanceInfo = connectionsConfig.create(config);

            assertNotNull(instanceInfo);
            assertEquals("https://domain:1234/swagger", instanceInfo.getMetadata().get("swaggerUrl"));
            assertEquals("otherValue", instanceInfo.getMetadata().get("otherKey"));
        }

    }

    @Nested
    class ConfigDelegator {

        private final EurekaInstanceConfig eurekaInstanceConfig = mock(EurekaInstanceConfig.class);
        private final InstanceInfo instanceInfo = mock(InstanceInfo.class);
        private final ConnectionsConfig.AdditionalEurekaConfiguration delegator = new ConnectionsConfig.AdditionalEurekaConfiguration(eurekaInstanceConfig, instanceInfo);

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void givenDelegator_whenGetHostName_thenCallConfigButReturnInstanceInfo(boolean refresh) {
            doReturn("hostname").when(instanceInfo).getHostName();
            assertEquals("hostname", delegator.getHostName(refresh));
            verify(eurekaInstanceConfig, times(1)).getHostName(refresh);
            verify(instanceInfo, times(1)).getHostName();
        }

        @Test
        void givenUnsecuredConfiguration_whenGetHealthCheckUrl_thenCallGetHealthCheckUrl() {
            doReturn(true).when(instanceInfo).isPortEnabled(InstanceInfo.PortType.UNSECURE);
            doReturn("unsecuredUrl").when(instanceInfo).getHealthCheckUrl();
            assertEquals("unsecuredUrl", delegator.getHealthCheckUrl());
        }

        @Test
        void givenSecuredConfiguration_whenGetHealthCheckUrl_thenCallGetSecureHealthCheckUrl() {
            doReturn(false).when(instanceInfo).isPortEnabled(InstanceInfo.PortType.UNSECURE);
            doReturn("securedUrl").when(instanceInfo).getSecureHealthCheckUrl();
            assertEquals("securedUrl", delegator.getHealthCheckUrl());
        }

        @Test
        void givenDelegator_whenGetSecureHealthCheckUrl_thenCallInstanceInfo() {
            doReturn("securedUrl").when(instanceInfo).getSecureHealthCheckUrl();
            assertEquals("securedUrl", delegator.getSecureHealthCheckUrl());
        }

        @Test
        void givenDelegator_whenGetHomePageUrl_thenCallInstanceInfo() {
            doReturn("homepageUrl").when(instanceInfo).getHomePageUrl();
            assertEquals("homepageUrl", delegator.getHomePageUrl());
        }

        @Test
        void givenDelegator_whenGetStatusPageUrl_thenCallInstanceInfo() {
            doReturn("statuspageUrl").when(instanceInfo).getStatusPageUrl();
            assertEquals("statuspageUrl", delegator.getStatusPageUrl());
        }

    }

}

