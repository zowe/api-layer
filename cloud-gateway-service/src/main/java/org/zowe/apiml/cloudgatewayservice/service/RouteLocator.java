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
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteLocator implements RouteDefinitionLocator {

    ReactiveDiscoveryClient discoveryClient;
    DiscoveryLocatorProperties properties;
    Flux<List<ServiceInstance>> services;

    public RouteLocator(ReactiveDiscoveryClient discoveryClient, DiscoveryLocatorProperties properties){
        this.discoveryClient = discoveryClient;
        this.services = discoveryClient.getServices().flatMap(serviceId -> discoveryClient.getInstances(serviceId).collectList());
        this.properties = properties;
    }
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return services.filter(instance -> !instance.isEmpty()).map(instances -> instances.get(0)).map(instance ->{
            RouteDefinition definition = new RouteDefinition();
            definition.setId("id");
            definition.setUri(URI.create("https://localhost:10012/"));
            List<FilterDefinition> filters = new ArrayList<>();
            FilterDefinition fd = new FilterDefinition("RewritePath=/service(?<segment>/?.*), /discoverableclient/${segment}");

            filters.add(fd);
//            PredicateDefinition pd = new PredicateDefinition("Path=/service/**");
            definition.setFilters(filters);
//            definition.setPredicates(Arrays.asList(pd));
            return definition;
        });
    }
}
