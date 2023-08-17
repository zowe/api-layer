/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.Expression;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.util.CorsUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class ProxyRouteLocator extends RouteLocator {

    public ProxyRouteLocator(ReactiveDiscoveryClient discoveryClient, DiscoveryLocatorProperties properties, List<FilterDefinition> filters, ApplicationContext context, CorsUtils corsUtils) {
        super(discoveryClient, properties, filters, context, corsUtils);

    }

    @Override
    protected void setProperties(RouteDefinition routeDefinition, ServiceInstance instance, RoutedService service) {
        PredicateDefinition predicate = new PredicateDefinition();

        predicate.setName("Header");
        predicate.addArg("header", "X-Request-Id");
        predicate.addArg("regexp", (instance.getServiceId() + instance.getHost()).toLowerCase(Locale.ROOT));

        routeDefinition.getPredicates().add(predicate);

        for (FilterDefinition filter : getFilters()) {
            routeDefinition.getFilters().add(filter);
        }
    }

    @Override
    protected RouteDefinition buildRouteDefinition(Expression urlExpr, ServiceInstance serviceInstance, String routeId) {
        String serviceId = serviceInstance.getInstanceId();
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(getRouteIdPrefix() + serviceId + routeId);
        String uri = String.format("%s://%s:%d", serviceInstance.getScheme(), serviceInstance.getHost(), serviceInstance.getPort());
        routeDefinition.setUri(URI.create(uri));
        // add instance metadata
        routeDefinition.setMetadata(new LinkedHashMap<>(serviceInstance.getMetadata()));
        return routeDefinition;
    }
}
