/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.routing.RoutedService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class NewApimlRouteLocator extends SimpleRouteLocator {

    private final DiscoveryClient discoveryClient;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    public NewApimlRouteLocator(String servletPath, ZuulProperties properties, DiscoveryClient discoveryClient) {
        super(servletPath, properties);
        this.discoveryClient = discoveryClient;
    }



    @Override
    @VisibleForTesting
    protected Map<String, ZuulProperties.ZuulRoute> locateRoutes() {
        log.debug("Locating routes from Discovery client");

        Map<String, ZuulProperties.ZuulRoute> routesMap = new HashMap<>();

        List<String> serviceIds = discoveryClient.getServices();
        for(String serviceId: serviceIds) {
            routesMap.putAll(locateServiceRoutes(serviceId));
        }

        return routesMap;
    }

    private Map<String, ZuulProperties.ZuulRoute> locateServiceRoutes(String serviceId) {

        Map<String, ZuulProperties.ZuulRoute> routesMap = new HashMap<>();

        discoveryClient.getInstances(serviceId).stream()
            .map(ServiceInstance::getMetadata)
            .flatMap(
                metadata -> metadataParser.parseToListRoute(metadata).stream()
            ).forEach( routedService -> {
                    routesMap.putAll(createIndividualRoute(serviceId, routedService));
            });

        return  routesMap;
    }

    private Map<String, ZuulProperties.ZuulRoute> createIndividualRoute(String serviceId, RoutedService routedService) {
        Map<String, ZuulProperties.ZuulRoute> routesMap = new HashMap<>();
        //strategy
        String routeKey = "/" + routedService.getGatewayUrl() + "/" + serviceId + "/**";
        ZuulProperties.ZuulRoute route = new ZuulProperties.ZuulRoute(routeKey, serviceId);
        routesMap.put(routeKey, route);

        log.debug("ServiceId: {}, RouteId: {}, Created Route: {}", serviceId, routedService.getSubServiceId(), route);
        return routesMap;
    }
}
