/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.health;

import org.zowe.apiml.product.constants.CoreService;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApiCatalogHealthIndicatorTest {

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final ApiCatalogHealthIndicator apiCatalogHealthIndicator = new ApiCatalogHealthIndicator(discoveryClient);
    private final Health.Builder builder = new Health.Builder();

    @Test
    public void testStatusIsUpWhenGatewayIsAvailable() {
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
            Collections.singletonList(
                new DefaultServiceInstance(CoreService.GATEWAY.getServiceId(), "host", 10010, true)));

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.UP, builder.build().getStatus());
    }

    @Test
    public void testStatusIsDownWhenGatewayIsNotAvailable() {
        when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(Collections.emptyList());

        apiCatalogHealthIndicator.doHealthCheck(builder);

        assertEquals(Status.DOWN, builder.build().getStatus());
    }
}
