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