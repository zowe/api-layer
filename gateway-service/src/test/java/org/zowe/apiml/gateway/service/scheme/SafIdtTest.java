/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service.scheme;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SafIdtTest {

    private static final String SERVICE_ID = "myservice";
    private static final String APPLID = "APPLID";
    private ServiceInstance serviceInstance;

    @BeforeAll
    void setup() {
        serviceInstance = mock(ServiceInstance.class);
        doReturn(SERVICE_ID).when(serviceInstance).getServiceId();
    }

    @ParameterizedTest
    @EmptySource
    @NullSource
    void givenNoApplid_whenApply_thenReturnFailoverFilter(String applid) {
        var routeDefinition = new RouteDefinition();

        new SafIdt().apply(serviceInstance, routeDefinition, Authentication.builder().scheme(AuthenticationScheme.SAF_IDT).applid(applid).build());

        assertEquals(1, routeDefinition.getFilters().size());
        var filter = routeDefinition.getFilters().get(0);
        assertEquals("RoutingConfigurationErrorFilterFactory", filter.getName());
        assertEquals(3, filter.getArgs().size());
        assertEquals(SERVICE_ID, filter.getArgs().get("serviceId"));
        assertEquals("APPLID is not configured", filter.getArgs().get("message"));
        assertEquals("safIdt", filter.getArgs().get("authenticationScheme"));
    }

    @Test
    void givenValidConfiguration_whenApply_thenReturnAuthFilter() {
        var routeDefinition = new RouteDefinition();

        new SafIdt().apply(serviceInstance, routeDefinition, Authentication.builder().scheme(AuthenticationScheme.SAF_IDT).applid(APPLID).build());

        assertEquals(1, routeDefinition.getFilters().size());
        var filter = routeDefinition.getFilters().get(0);
        assertEquals("SafIdtFilterFactory", filter.getName());
        assertEquals(2, filter.getArgs().size());
        assertEquals(APPLID, filter.getArgs().get("applicationName"));
        assertEquals(SERVICE_ID, filter.getArgs().get("serviceId"));
    }

}
