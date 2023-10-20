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
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClientFactory;

import java.util.ArrayList;
import java.util.TreeMap;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

@ExtendWith(MockitoExtension.class)
class DiscoveryClientConfigTest {
    @Nested
    class WhenProcessingAdditionalRegistrations {
        @Mock
        private EurekaJerseyClientImpl.EurekaJerseyClientBuilder eurekaJerseyClientBuilder;
        @Mock
        private ApplicationInfoManager appManager;
        @Mock
        private ApimlDiscoveryClient discoveryClientClient;
        @Mock
        private ApimlDiscoveryClientFactory apimlDiscoveryClientFactory;
        @Captor
        private ArgumentCaptor<ApplicationInfoManager> appInfoManagerCaptor;
        @Mock
        private EurekaClientConfig eurekaClientConfig;
        @Mock
        private HealthCheckHandler healthCheckHandler;

        private AdditionalRegistration registration;
        private InstanceInfo instanceInfo;
        @Mock
        private InstanceInfo baseInstanceInfo;
        @InjectMocks
        private DiscoveryClientConfig discoveryClientConfig;

        @BeforeEach
        public void setUp() {
            registration = AdditionalRegistration.builder()
                .discoveryServiceUrls("https://host:10011/eureka").routes(new ArrayList<>()).build();

            instanceInfo = new InstanceInfo.Builder(baseInstanceInfo).setAppName(GATEWAY.name()).setMetadata(new TreeMap<>()).build();
            when(appManager.getInfo()).thenReturn(instanceInfo);
            when(apimlDiscoveryClientFactory.buildApimlDiscoveryClient(any(), any(), any(), any())).thenReturn(discoveryClientClient);

        }

        @Test
        void shouldRegisterHealthCheckHandler() {
            DiscoveryClientWrapper discoveryClientWrapper = discoveryClientConfig.additionalDiscoveryClientWrapper(appManager, eurekaClientConfig, healthCheckHandler, singletonList(registration));

            assertThat(discoveryClientWrapper.getDiscoveryClients()).hasSize(1);
            verify(discoveryClientClient).registerHealthCheck(healthCheckHandler);
        }

        @Test
        void shouldAddAdditionalRoutesToMetadata() {

            registration.getRoutes().add(new AdditionalRegistration.Route("/gatewayUrl", "/serviceUrl"));

            discoveryClientConfig.additionalDiscoveryClientWrapper(appManager, eurekaClientConfig, healthCheckHandler, singletonList(registration));

            verify(apimlDiscoveryClientFactory).buildApimlDiscoveryClient(appInfoManagerCaptor.capture(), any(), any(), any());

            ApplicationInfoManager createdInfoManager = appInfoManagerCaptor.getValue();
            InstanceInfo info = createdInfoManager.getInfo();
            when(info.getMetadata()).thenCallRealMethod();
            assertThat(info.getMetadata()).containsEntry("apiml.routes.0.gatewayUrl", "/gatewayUrl");
            assertThat(info.getMetadata()).containsEntry("apiml.routes.0.serviceUrl", "/serviceUrl");
        }

        @Test
        void shouldNotAddAdditionalRoutesToMetadata() {

            discoveryClientConfig.additionalDiscoveryClientWrapper(appManager, eurekaClientConfig, healthCheckHandler, singletonList(registration));

            verify(apimlDiscoveryClientFactory).buildApimlDiscoveryClient(appInfoManagerCaptor.capture(), any(), any(), any());

            ApplicationInfoManager createdInfoManager = appInfoManagerCaptor.getValue();
            InstanceInfo info = createdInfoManager.getInfo();
            when(info.getMetadata()).thenCallRealMethod();
            assertThat(info.getMetadata()).isEmpty();
        }

    }

}
