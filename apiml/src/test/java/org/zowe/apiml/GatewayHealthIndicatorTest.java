/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.ApiCatalogServiceAvailableEvent;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayHealthIndicatorTest {

    @Mock private DiscoveryClient discoveryClient;
    @Mock private ApplicationContext applicationContext;
    @Mock private ServiceStartupEventHandler serviceStartupEventHandler;

    private GatewayHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new GatewayHealthIndicator(applicationContext, serviceStartupEventHandler);
        ReflectionTestUtils.setField(healthIndicator, "apiCatalogServiceId", CoreService.API_CATALOG.getServiceId());
        lenient().when(applicationContext.getBean(DiscoveryClient.class)).thenReturn(discoveryClient);
    }

    private DefaultServiceInstance getDefaultServiceInstance(String serviceId, String hostname, int port) {
        return new DefaultServiceInstance(
            hostname + ":" + serviceId + ":" + port,
            serviceId, hostname, port, true
        );
    }

    @Nested
    class WhenCatalogAndDiscoveryAreAvailable {

        @BeforeEach
        void setUp() {
            healthIndicator.onApplicationEvent(new EurekaRegistryAvailableEvent(mock(EurekaServerConfig.class)));
            healthIndicator.onApplicationEvent(new ZaasServiceAvailableEvent("dummy"));
            healthIndicator.onApplicationEvent(new ApiCatalogServiceAvailableEvent(new Object()));
        }

        @Test
        void testStatusIsUp() throws Exception {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.GATEWAY.getServiceId(), "host", 10010)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);
            assertEquals(Status.UP, builder.build().getStatus());
        }

    }

    @Nested
    class WhenDiscoveryIsNotAvailable {

        @BeforeEach
        void setUp() {
            healthIndicator.onApplicationEvent(new ZaasServiceAvailableEvent("dummy"));
        }

        @Test
        void thenStatusIsDown() throws Exception {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.GATEWAY.getServiceId(), "host", 10010)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);
            assertEquals(Status.DOWN, builder.build().getStatus());
        }

        @Test
        void whenClientNotAvailable_thenDoNothing() throws Exception {
            when(applicationContext.getBean(DiscoveryClient.class)).thenThrow(new NoSuchBeanDefinitionException(DiscoveryClient.class));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);

            verifyNoInteractions(serviceStartupEventHandler);
            verifyNoInteractions(discoveryClient);
        }

    }

    @Nested
    class WhenZaasIsNotAvailable {

        @BeforeEach
        void setUp() {
            healthIndicator.onApplicationEvent(new EurekaRegistryAvailableEvent(mock(EurekaServerConfig.class)));
        }

        @Test
        void thenStatusIsDown() throws Exception {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.GATEWAY.getServiceId(), "host", 10010)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);
            assertEquals(Status.DOWN, builder.build().getStatus());
        }

    }

    @Nested
    class GivenCustomCatalogProvider {

        @Test
        void whenHealthIsRequested_thenStatusIsUp() throws Exception {
            var customCatalogServiceId = "customCatalog";
            ReflectionTestUtils.setField(healthIndicator, "apiCatalogServiceId", customCatalogServiceId);

            when(discoveryClient.getInstances(customCatalogServiceId)).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(customCatalogServiceId, "host", 10014)));
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.GATEWAY.getServiceId(), "host", 10010)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);

            String code = (String) builder.build().getDetails().get(CoreService.API_CATALOG.getServiceId());
            assertEquals("UP", code);
        }

    }

    @Nested
    class GivenEverythingIsHealthy {

        @BeforeEach
        void setUp() {
            healthIndicator.onApplicationEvent(new EurekaRegistryAvailableEvent(mock(EurekaServerConfig.class)));
            healthIndicator.onApplicationEvent(new ZaasServiceAvailableEvent("dummy"));
        }

        @Test
        void whenHealthRequested_onceLogMessageAboutStartup() throws Exception {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);

            assertTrue(healthIndicator.isStartedInformationPublished());
        }

    }

    @Nested
    class OnCatalogRegistration {

        @Test
        void whenBothEvents_thenOneMessage() {
            var registeredEvent = mock(EurekaInstanceRegisteredEvent.class);

            var instanceInfo = mock(InstanceInfo.class);
            when(registeredEvent.getInstanceInfo()).thenReturn(instanceInfo);
            when(instanceInfo.getAppName()).thenReturn("apicatalog");

            doNothing().when(serviceStartupEventHandler).onServiceStartup("API Catalog Service", 5);

            healthIndicator.onApplicationEvent(new ApiCatalogServiceAvailableEvent(new Object()));
            healthIndicator.onApplicationEvent(registeredEvent);

            verify(serviceStartupEventHandler, times(1)).onServiceStartup("API Catalog Service", 5);
        }

        @Test
        void whenBothEventsReverse_thenOneMessage() {
            var registeredEvent = mock(EurekaInstanceRegisteredEvent.class);

            var instanceInfo = mock(InstanceInfo.class);
            when(registeredEvent.getInstanceInfo()).thenReturn(instanceInfo);
            when(instanceInfo.getAppName()).thenReturn("apicatalog");

            doNothing().when(serviceStartupEventHandler).onServiceStartup("API Catalog Service", 5);

            healthIndicator.onApplicationEvent(registeredEvent);
            healthIndicator.onApplicationEvent(new ApiCatalogServiceAvailableEvent(new Object()));

            verify(serviceStartupEventHandler, times(1)).onServiceStartup("API Catalog Service", 5);
        }

    }

}
