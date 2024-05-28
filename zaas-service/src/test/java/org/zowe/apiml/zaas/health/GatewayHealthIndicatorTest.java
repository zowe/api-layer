/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.product.constants.CoreService;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayHealthIndicatorTest {

    private Providers providers;

    @BeforeEach
    void setUp() {
        providers = mock(Providers.class);
    }

    private DefaultServiceInstance getDefaultServiceInstance(String serviceId, String hostname, int port) {
        return new DefaultServiceInstance(
            hostname + ":" + serviceId + ":" + port,
            serviceId, hostname, port, true
        );
    }

    @Test
    void testStatusIsUpWhenCatalogAndDiscoveryAreAvailable() {
        when(providers.isZosfmUsed()).thenReturn(false);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, providers, CoreService.API_CATALOG.getServiceId());
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    void testStatusIsDownWhenDiscoveryIsNotAvailable() {
        when(providers.isZosfmUsed()).thenReturn(false);
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(Collections.emptyList());

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, providers, CoreService.API_CATALOG.getServiceId());
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.DOWN, builder.build().getStatus());
    }

    @Test
    void givenZosmfIsUsedAnfZosmfIsUnavailable_whenHealthIsRequested_thenStatusIsDown() {
        when(providers.isZosfmUsed()).thenReturn(true);
        when(providers.isZosmfAvailableAndOnline()).thenReturn(false);

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(Collections.emptyList());

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, providers, CoreService.API_CATALOG.getServiceId());
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.DOWN, builder.build().getStatus());
    }

    @Test
    void givenZosmfIsUsedAndAvailable_whenHealthIsRequested_thenStatusIsUp() {
        when(providers.isZosfmUsed()).thenReturn(true);
        when(providers.isZosmfAvailableAndOnline()).thenReturn(true);

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, providers, CoreService.API_CATALOG.getServiceId());
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    void givenCustomCatalogProvider_whenHealthIsRequested_thenStatusIsUp() {
        when(providers.isZosfmUsed()).thenReturn(true);
        when(providers.isZosmfAvailableAndOnline()).thenReturn(true);
        String customCatalogServiceId = "customCatalog";

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(customCatalogServiceId)).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(customCatalogServiceId, "host", 10014)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, providers, customCatalogServiceId);
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);

        String code = (String) builder.build().getDetails().get(CoreService.API_CATALOG.getServiceId());
        assertThat(code, is("UP"));
    }

    @Test
    void givenEverythingIsHealthy_whenHealthRequested_onceLogMessageAboutStartup() {
        when(providers.isZosfmUsed()).thenReturn(true);
        when(providers.isZosmfAvailableAndOnline()).thenReturn(true);
        
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(getDefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011)));

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, providers, CoreService.API_CATALOG.getServiceId());
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);

        assertThat(gatewayHealthIndicator.startedInformationPublished, is(true));
    }
}
