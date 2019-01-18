/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.health;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import com.ca.mfaas.product.constants.CoreService;

import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

public class ApimlHealthIndicatorTest {

    @Test
    public void testStatusIsUpWhenCatalogAndDiscoveryAreAvailable() throws Exception {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(
                Arrays.asList(new DefaultServiceInstance(CoreService.API_CATALOG.getServiceId(), "host", 10014, true)));
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
                Arrays.asList(new DefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10014, true)));

        ApimlHealthIndicator apimlHealthIndicator = new ApimlHealthIndicator(discoveryClient);
        Health.Builder builder = new Health.Builder();
        apimlHealthIndicator.doHealthCheck(builder);
        assertEquals(builder.build().getStatus(), Status.UP);
    }

    @Test
    public void testStatusIsDownWhenCatalogIsNotAvailable() throws Exception {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId())).thenReturn(new ArrayList<>());
        when(discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId())).thenReturn(
                Arrays.asList(new DefaultServiceInstance(CoreService.DISCOVERY.getServiceId(), "host", 10014, true)));

        ApimlHealthIndicator apimlHealthIndicator = new ApimlHealthIndicator(discoveryClient);
        Health.Builder builder = new Health.Builder();
        apimlHealthIndicator.doHealthCheck(builder);
        assertEquals(builder.build().getStatus(), Status.DOWN);
    }
}
