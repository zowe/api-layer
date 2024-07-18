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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class X509Test {

    @Test
    void givenX509_whenGetAuthenticationScheme_thenReturnProperType() {
        assertEquals(AuthenticationScheme.X509, new X509().getAuthenticationScheme());
    }

    @Test
    void givenX509_whenApply_thenFulfillFilterFactorArgs() {
        RouteDefinition routeDefinition = new RouteDefinition();
        Authentication authentication = new Authentication();
        authentication.setHeaders("header1,header2");
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        doReturn("service").when(serviceInstance).getServiceId();

        new X509().apply(serviceInstance, routeDefinition, authentication);

        assertEquals(1, routeDefinition.getFilters().size());
        FilterDefinition filterDefinition = routeDefinition.getFilters().get(0);
        assertEquals("header1,header2", filterDefinition.getArgs().get("headers"));
        assertEquals("X509FilterFactory", filterDefinition.getName());
    }

}
