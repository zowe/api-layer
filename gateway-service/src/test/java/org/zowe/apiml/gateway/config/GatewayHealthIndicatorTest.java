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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.product.constants.CoreService;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayHealthIndicatorTest {

    @Mock
    private DiscoveryClient discoveryClient;

    private GatewayHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        this.healthIndicator = new GatewayHealthIndicator(discoveryClient, CoreService.API_CATALOG.getServiceId());
    }

    private DefaultServiceInstance getDefaultServiceInstance(String serviceId, String hostname, int port) {
        return new DefaultServiceInstance(
            hostname + ":" + serviceId + ":" + port,
            serviceId, hostname, port, true
        );
    }

    @Nested
    class WhenCatalogAndDiscoveryAreAvailable {
        @Test
        void testStatusIsUp() {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));
            when(discoveryClient.getInstances(CoreService.ZAAS.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.ZAAS.getServiceId(), "host", 10011)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);
            assertEquals(Status.UP, builder.build().getStatus());
        }
    }

    @Nested
    class WhenDiscoveryIsNotAreAvailable {
        @Test
        void testStatusIsDown() {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(Collections.emptyList());

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);
            assertEquals(Status.DOWN, builder.build().getStatus());
        }
    }

    @Nested
    class GivenCustomCatalogProvider {

        @Test
        void whenHealthIsRequested_thenStatusIsUp() {
            String customCatalogServiceId = "customCatalog";

            when(discoveryClient.getInstances(customCatalogServiceId)).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(customCatalogServiceId, "host", 10014)));
            when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));

            var healthIndicator = new GatewayHealthIndicator(discoveryClient, customCatalogServiceId);

            Health.Builder builder = new Health.Builder();
            healthIndicator.doHealthCheck(builder);

            String code = (String) builder.build().getDetails().get(CoreService.API_CATALOG.getServiceId());
            assertThat(code, is("UP"));
        }

    }

    @Nested
    class GivenEverythingIsHealthy {

        @Test
        void whenHealthRequested_onceLogMessageAboutStartup() {
            when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
            when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));
            when(discoveryClient.getInstances(CoreService.ZAAS.getServiceId())).thenReturn(
                Collections.singletonList(getDefaultServiceInstance(CoreService.ZAAS.getServiceId(), "host", 10023)));

            Health.Builder builder = new Health.Builder();
            healthIndicator.onApplicationEvent(mock(ApplicationReadyEvent.class));

            healthIndicator.doHealthCheck(builder);

            assertThat(healthIndicator.isStartedInformationPublished(), is(true));
        }

    }

}
