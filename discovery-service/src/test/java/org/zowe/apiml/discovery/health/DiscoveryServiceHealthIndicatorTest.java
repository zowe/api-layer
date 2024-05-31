/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.health;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.product.constants.CoreService;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryServiceHealthIndicatorTest {

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final DiscoveryServiceHealthIndicator discoverServiceHealthIndicator = new DiscoveryServiceHealthIndicator(discoveryClient);
    private final Health.Builder builder = new Health.Builder();

    @Nested
    class GivenGatewayStatus {

        @Test
        void statusIsUpWhenGatewayIsAvailable() {
            when(discoveryClient.getInstances(CoreService.ZAAS.getServiceId())).thenReturn(
                Collections.singletonList(
                    new DefaultServiceInstance(null, CoreService.ZAAS.getServiceId(), "host", 10010, true)));

            discoverServiceHealthIndicator.doHealthCheck(builder);

            Assertions.assertEquals(Status.UP, builder.build().getStatus());
            Assertions.assertEquals(Status.UP, builder.build().getDetails().get("zaas"));
        }

        @Test
        void statusIsPartialWhenGatewayIsNotAvailable() {
            when(discoveryClient.getInstances(CoreService.ZAAS.getServiceId())).thenReturn(Collections.emptyList());

            discoverServiceHealthIndicator.doHealthCheck(builder);

            Assertions.assertEquals(new Status("PARTIAL"), builder.build().getStatus());
            Assertions.assertEquals("Authenticated endpoints not available.", builder.build().getStatus().getDescription());
            Assertions.assertEquals(Status.DOWN, builder.build().getDetails().get("zaas"));
        }

    }

}
