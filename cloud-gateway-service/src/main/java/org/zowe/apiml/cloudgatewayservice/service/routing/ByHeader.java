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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.routing.RoutedService;

/**
 * The routing rule by header. It uses a header with name {@link ByHeader#TARGET_HEADER_NAME} and make rule routing by
 * it. It looks for a first part of routing steps and redirect by it (it could contain multiple steps separated by /).
 *
 * The rule modify the header for next hop (remove first part of remove the whole one if there is just one part).
 */
@Component
public class ByHeader extends RouteDefinitionProducer {

    private static final String TARGET_HEADER_NAME = "X-Request-Id";

    public ByHeader(DiscoveryLocatorProperties properties) {
        super(properties);
    }

    @Override
    protected void setCondition(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        PredicateDefinition predicate = new PredicateDefinition();

        predicate.setName("Header");
        predicate.addArg("header", TARGET_HEADER_NAME);
        predicate.addArg("regexp", serviceInstance.getServiceId() + "(/.*)?");

        routeDefinition.getPredicates().add(predicate);
    }

    @Override
    protected void setFilters(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
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
