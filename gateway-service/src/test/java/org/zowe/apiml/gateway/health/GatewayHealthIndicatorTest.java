/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import org.zowe.apiml.product.constants.CoreService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayHealthIndicatorTest {

    private static final String ZOSMF = "zosmf";

    private ZosmfService zosmfService;

    @BeforeEach
    void setUp() {
        zosmfService = mock(ZosmfService.class);
        when(zosmfService.isUsed()).thenReturn(false);
    }

    @Test
    void testStatusIsUpWhenCatalogAndDiscoveryAreAvailable() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014, true)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10011, true)));
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(ZOSMF, "host", 10050, true)));

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, zosmfService);
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    void testStatusIsDownWhenDiscoveryIsNotAvailable() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
            Collections.singletonList(new DefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014, true)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(Collections.emptyList());
        when(discoveryClient.getInstances(ZOSMF)).thenReturn(Collections.emptyList());

        GatewayHealthIndicator gatewayHealthIndicator = new GatewayHealthIndicator(discoveryClient, zosmfService);
        Health.Builder builder = new Health.Builder();
        gatewayHealthIndicator.doHealthCheck(builder);
        assertEquals(Status.DOWN, builder.build().getStatus());
    }
}
