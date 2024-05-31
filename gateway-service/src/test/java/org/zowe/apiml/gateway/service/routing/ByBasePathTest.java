/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service.routing;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.product.routing.RoutedService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ByBasePathTest {

    private static final String TARGET_HEADER_NAME = "X-Forward-To";

    @Nested
    class CommonParts {

        @ParameterizedTest(name = "constructUrl([{0}, {0}] returns {1}")
        @CsvSource({
            "a,/a/a",
            "/a,/a/a",
            "a/,/a/a",
            "/a/,/a/a",
            "//,''"
        })
        void givenUrlPath_whenConstructUrl_thenFixSlashes(String part, String path) {
            assertEquals(path, ByBasePath.constructUrl(part, part));
        }

        @Test
        void givenInstance_whenOrder_thenReturnsTheDefaultOrder() {
            assertEquals(1, new ByBasePath(new DiscoveryLocatorProperties()).getOrder());
        }

    }

    @Nested
    class RuleConstruction {

        @ParameterizedTest(name = "condition for service {0} with gatewayUrl {1} is {2}")
        @CsvSource({
            "service,/path/,/service/path/**",
            "service,path,/service/path/**"
        })
        void givenInstanceConfig_whenSetCondition_thenConstructRegexCondition(
            String serviceId, String gatewayUrl, String pattern
        ) {
            RouteDefinition routeDefinition = new RouteDefinition();
            ServiceInstance serviceInstance = mock(ServiceInstance.class);
            doReturn(serviceId).when(serviceInstance).getServiceId();
            RoutedService routedService = new RoutedService(null, gatewayUrl, null);

            new ByBasePath(new DiscoveryLocatorProperties()).setCondition(routeDefinition, serviceInstance, routedService);

            assertEquals(2, routeDefinition.getPredicates().size());
            PredicateDefinition headerPredicate = routeDefinition.getPredicates().get(0);
            assertEquals("MissingHeader", headerPredicate.getName());
            assertEquals(TARGET_HEADER_NAME, headerPredicate.getArgs().get("header"));

            PredicateDefinition pathPredicate = routeDefinition.getPredicates().get(1);
            assertEquals("Path", pathPredicate.getName());
            assertEquals(pattern, pathPredicate.getArgs().get("pattern"));
        }

        @ParameterizedTest(name = "to map URLs of service {0} from {1} to {2} is constructed pattern {3} and replacement {4} arguments")
        @CsvSource({
            "service,/api/v1/,/x/,/service/api/v1/?(?<remaining>.*),/x/${remaining}",
            "service,api/v1,x,/service/api/v1/?(?<remaining>.*),/x/${remaining}",
        })
        void givenInstanceConfig_whenSetFilters_thenConstructRegexFilter(
                String serviceId, String gatewayUrl, String serviceUrl, String pattern, String replacement
        ) {
            RouteDefinition routeDefinition = new RouteDefinition();
            ServiceInstance serviceInstance = mock(ServiceInstance.class);
            doReturn(serviceId).when(serviceInstance).getServiceId();
            RoutedService routedService = new RoutedService(null, gatewayUrl, serviceUrl);

            new ByBasePath(new DiscoveryLocatorProperties()).setFilters(routeDefinition, serviceInstance, routedService);

            assertEquals(1, routeDefinition.getFilters().size());
            FilterDefinition filterDefinition = routeDefinition.getFilters().get(0);
            assertEquals("RewritePath", filterDefinition.getName());
            assertEquals(pattern, filterDefinition.getArgs().get("regexp"));
            assertEquals(replacement, filterDefinition.getArgs().get("replacement"));
        }

    }

}
