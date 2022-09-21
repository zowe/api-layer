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
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.zowe.apiml.product.routing.RoutedService;

import java.util.Locale;

public class ProxyRouteLocator extends RouteLocator {


    public ProxyRouteLocator(ReactiveDiscoveryClient discoveryClient, DiscoveryLocatorProperties properties) {
        super(discoveryClient, properties);
    }

    protected void setProperties(RouteDefinition routeDefinition, ServiceInstance instance, RoutedService service) {
        PredicateDefinition predicate = new PredicateDefinition();
        predicate.setName("Header");
        predicate.addArg("header", "X-Request-Id");
        predicate.addArg("regexp", instance.getServiceId().toLowerCase(Locale.ROOT));
        routeDefinition.getPredicates().add(predicate);

    }
}
