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
import org.zowe.apiml.util.StringUtils;

/**
 * Routing rule by path modify the path of the request. It makes this replacement:
 *
 *  from: /<serviceId>/<gatewayUrl>/<path>
 *  to: /<serviceUrl>/<path>
 *
 */
@Component
public class ByBasePath extends RouteDefinitionProducer {

    private static final String TARGET_HEADER_NAME = "X-Forward-To";

    public ByBasePath(DiscoveryLocatorProperties properties) {
        super(properties);
    }

    static String constructUrl(String...parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            part = StringUtils.removeFirstAndLastOccurrence(part, "/");
            if (part.isEmpty()) continue;

            sb.append('/');
            sb.append(part);
        }
        return sb.toString();
    }

    @Override
    protected void setCondition(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        PredicateDefinition headerPredicate = new PredicateDefinition();
        headerPredicate.setName("MissingHeader");
        headerPredicate.addArg("header", TARGET_HEADER_NAME);
        routeDefinition.getPredicates().add(headerPredicate);

        PredicateDefinition pathPredicate = new PredicateDefinition();
        pathPredicate.setName("Path");
        String predicateValue = constructUrl(serviceInstance.getServiceId(), routedService.getGatewayUrl(), "**");
        pathPredicate.addArg("pattern", predicateValue);
        routeDefinition.getPredicates().add(pathPredicate);
    }

    @Override
    protected void setFilters(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        FilterDefinition filter = new FilterDefinition();
        filter.setName("RewritePath");

        filter.addArg("regexp", constructUrl(serviceInstance.getServiceId(), routedService.getGatewayUrl(), "?(?<remaining>.*)"));
        filter.addArg("replacement", constructUrl(routedService.getServiceUrl(), "${remaining}"));

        routeDefinition.getFilters().add(filter);
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
