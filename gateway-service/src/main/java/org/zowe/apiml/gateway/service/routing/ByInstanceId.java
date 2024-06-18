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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.routing.RoutedService;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * The routing rule for deterministic routing. If the request has set header `X-InstanceId`
 * {@link ByInstanceId#HEADER_NAME} it route directly to this instanceId. If the instanceId is not available then
 * it would be ignored.
 */
@Component
public class ByInstanceId extends RouteDefinitionProducer {

    private static final String HEADER_NAME = "X-InstanceId";


    public ByInstanceId(DiscoveryLocatorProperties properties) {
        super(properties);
    }

    @Override
    protected void setCondition(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        PredicateDefinition predicate = new PredicateDefinition();

        predicate.setName("Header");
        predicate.addArg("header", HEADER_NAME);
        predicate.addArg("regexp", Pattern.quote(serviceInstance.getInstanceId()).toString());

        routeDefinition.getPredicates().add(predicate);
    }

    @Override
    protected void setFilters(RouteDefinition routeDefinition, ServiceInstance serviceInstance, RoutedService routedService) {
        routeDefinition.setUri(serviceInstance.getUri());
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
