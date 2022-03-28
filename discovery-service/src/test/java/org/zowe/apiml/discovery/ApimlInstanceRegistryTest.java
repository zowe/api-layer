/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery;

import com.netflix.appinfo.*;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.*;
import com.netflix.eureka.resources.ServerCodecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Nested;

import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class ApimlInstanceRegistryTest {
    private ApimlInstanceRegistry apimlInstanceRegistry;

    private EurekaServerConfig serverConfig;
    private EurekaClientConfig clientConfig;
    private ServerCodecs serverCodecs;
    private EurekaClient eurekaClient;
    private InstanceRegistryProperties instanceRegistryProperties;
    private ApplicationContext appCntx;

    @BeforeEach
    void setUp() {
        serverConfig = new DefaultEurekaServerConfig();
        clientConfig = mock(EurekaClientConfig.class);
        serverCodecs = mock(ServerCodecs.class);
        eurekaClient = mock(DiscoveryClient.class);
        instanceRegistryProperties = mock(InstanceRegistryProperties.class);
        appCntx = mock(ApplicationContext.class);
        apimlInstanceRegistry = new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            instanceRegistryProperties,
            appCntx);
    }

    @Nested
    class GivenRegexReplacer {
        @Nested
        class WhenChangeServiceId {
            @Test
            void thenChangeServicePrefix() {
                String regex = "service,hello";
                InstanceInfo info = apimlInstanceRegistry.changeServiceId(getStandardInstance(), regex);
                assertEquals("hello", info.getInstanceId());
                assertEquals("HELLO", info.getAppName());
                assertEquals("HELLO", info.getVIPAddress());
                assertEquals("HELLO", info.getAppGroupName());
                assertEquals("192.168.0.1", info.getIPAddr());
                assertEquals("localhost", info.getHostName());
                assertEquals(9090, info.getSecurePort());
                assertEquals("localhost", info.getSecureVipAddress());
            }
        }

        @Nested
        class WhenInstanceIdIsDifferentFromRegex {
            @Test
            void thenDontChangeServicePrefix() {
                String regex = "differentService,hello";
                InstanceInfo info = apimlInstanceRegistry.changeServiceId(getStandardInstance(), regex);
                assertEquals("service", info.getInstanceId());
                assertEquals("SERVICE", info.getAppName());
                assertEquals("SERVICE", info.getVIPAddress());
                assertEquals("SERVICE", info.getAppGroupName());
            }
        }
    }

    private InstanceInfo getStandardInstance() {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId("service")
            .setAppName("SERVICE")
            .setAppGroupName("SERVICE")
            .setIPAddr("192.168.0.1")
            .enablePort(InstanceInfo.PortType.SECURE, true)
            .setSecurePort(9090)
            .setHostName("localhost")
            .setSecureVIPAddress("localhost")
            .setVIPAddress("SERVICE")
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .build();
    }
}
