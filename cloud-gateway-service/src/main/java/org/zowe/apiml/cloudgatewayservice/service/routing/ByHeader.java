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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.routing.RoutedService;

@Component
public class ByHeader extends RouteDefinitionProducer {

    private static final String TARGET_HEADER_NAME = "X-Request-Id";

    public ByHeader(DiscoveryLocatorProperties properties) {
        super(properties);
    }

    @Override
    public void setCondition(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        PredicateDefinition predicate = new PredicateDefinition();

        predicate.setName("Header");
        predicate.addArg("header", TARGET_HEADER_NAME);
        predicate.addArg("regexp", serviceInstance.getServiceId() + "(/.*)?");

        routeDefinition.getPredicates().add(predicate);
    }

    @Override
    public void setFilters(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName("HeaderRouteStepFilterFactory");
        filter.addArg("header", TARGET_HEADER_NAME);
        routeDefinition.getFilters().add(filter);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
