/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service.routing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ByHeaderTest {

    @Nested
    class CommonParts {

        @Test
        void givenInstance_whenOrder_thenReturnsTheDefaultOrder() {
            assertEquals(0, new ByHeader(new DiscoveryLocatorProperties()).getOrder());
        }

    }

    @Nested
    class RuleConstruction {

        private static final String TARGET_HEADER_NAME = "X-Request-Id";

        @Test
        void givenInstanceConfig_whenSetCondition_thenConstructRegexCondition() {
            RouteDefinition routeDefinition = new RouteDefinition();
            ServiceInstance serviceInstance = mock(ServiceInstance.class);
            doReturn("myservice").when(serviceInstance).getServiceId();

            new ByHeader(new DiscoveryLocatorProperties()).setCondition(routeDefinition, serviceInstance, null);

            assertEquals(1, routeDefinition.getPredicates().size());
            PredicateDefinition predicateDefinition = routeDefinition.getPredicates().get(0);
            assertEquals("Header", predicateDefinition.getName());
            assertEquals(TARGET_HEADER_NAME, predicateDefinition.getArgs().get("header"));
            assertEquals("myservice(/.*)?", predicateDefinition.getArgs().get("regexp"));
        }

        @Test
        void givenInstanceConfig_whenSetFilters_thenConstructHeaderRouteStepFilter() {
            RouteDefinition routeDefinition = new RouteDefinition();

            new ByHeader(new DiscoveryLocatorProperties()).setFilters(routeDefinition, null, null);

            assertEquals(1, routeDefinition.getFilters().size());
            FilterDefinition filterDefinition = routeDefinition.getFilters().get(0);
            assertEquals("HeaderRouteStepFilterFactory", filterDefinition.getName());
            assertEquals(TARGET_HEADER_NAME, filterDefinition.getArgs().get("header"));
        }

    }

}
