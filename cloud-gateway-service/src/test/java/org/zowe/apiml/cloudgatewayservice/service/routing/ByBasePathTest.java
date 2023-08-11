/*
 * Copyright (c) 2022 Broadcom.  All Rights Reserved.  The term
 * "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This software and all information contained therein is
 * confidential and proprietary and shall not be duplicated,
 * used, disclosed, or disseminated in any way except as
 * authorized by the applicable license agreement, without the
 * express written permission of Broadcom.  All authorized
 * reproductions must be marked with this language.
 *
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO
 * THE EXTENT PERMITTED BY APPLICABLE LAW, BROADCOM PROVIDES THIS
 * SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL BROADCOM
 * BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR
 * DAMAGE, DIRECT OR INDIRECT, FROM THE USE OF THIS SOFTWARE,
 * INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS
 * INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF BROADCOM IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
 */

package org.zowe.apiml.cloudgatewayservice.service.routing;

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

            assertEquals(1, routeDefinition.getPredicates().size());
            PredicateDefinition predicateDefinition = routeDefinition.getPredicates().get(0);
            assertEquals("Path", predicateDefinition.getName());
            assertEquals(pattern, predicateDefinition.getArgs().get("pattern"));
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