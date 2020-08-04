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
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.DiscoveryClientRouteLocator;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class NewApimlRouteLocator extends DiscoveryClientRouteLocator {

    private final DiscoveryClient discoveryClient;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    private RoutedServicesNotifier routedServicesNotifier;

    public NewApimlRouteLocator(String servletPath, ZuulProperties properties, DiscoveryClient discoveryClient, RoutedServicesNotifier notifier) {
        super(servletPath, discoveryClient, properties, null, null);
        this.discoveryClient = discoveryClient;
        routedServicesNotifier = notifier;
    }

    @Override
    @VisibleForTesting
    protected LinkedHashMap<String, ZuulProperties.ZuulRoute> locateRoutes() {
        log.debug("Locating routes from Discovery client");
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();

        discoveryClient.getServices().forEach( serviceId ->
             routesMap.putAll(createServiceRoutes(serviceId))
        );

        routedServicesNotifier.notifyAndFlush();
        return routesMap;
    }

    private Map<String, ZuulProperties.ZuulRoute> createServiceRoutes(String serviceId) {
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();

        RoutedServices routedServices = new RoutedServices();

        discoveryClient.getInstances(serviceId).stream()
            .map(ServiceInstance::getMetadata)
            .flatMap(
                metadata -> metadataParser.parseToListRoute(metadata).stream()
            ).forEach( routedService -> {
                    routesMap.putAll(buildRoute(serviceId, routedService));
                    routedServices.addRoutedService(routedService);
            });

        routedServicesNotifier.addRoutedServices(serviceId, routedServices);
        return  routesMap;
    }

    private Map<String, ZuulProperties.ZuulRoute> buildRoute(String serviceId, RoutedService routedService) {
        LinkedHashMap<String, ZuulProperties.ZuulRoute> routesMap = new LinkedHashMap<>();

        String routeKey = "/" + routedService.getGatewayUrl() + "/" + serviceId + "/**";
        ZuulProperties.ZuulRoute route = new ZuulProperties.ZuulRoute(routeKey, serviceId);
        routesMap.put(routeKey, route);
        log.debug("ServiceId: {}, RouteId: {}, Created Route: {}", serviceId, routedService.getSubServiceId(), route);

        return routesMap;
    }
}
