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

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.cloudgatewayservice.service.routing.RouteDefinitionProducer;
import org.zowe.apiml.cloudgatewayservice.service.scheme.SchemeHandler;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.util.CorsUtils;
import org.zowe.apiml.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

@Service
public class RouteLocator implements RouteDefinitionLocator {

    private static final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    private final ApplicationContext context;

    private final CorsUtils corsUtils;
    private final ReactiveDiscoveryClient discoveryClient;

    private final List<FilterDefinition> commonFilters;
    private final List<RouteDefinitionProducer> routeDefinitionProducers;
    private final Map<AuthenticationScheme, SchemeHandler> schemeHandlers = new EnumMap<>(AuthenticationScheme.class);

    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final UrlBasedCorsConfigurationSource corsConfigurationSource = context.getBean(UrlBasedCorsConfigurationSource.class);

    public RouteLocator(
        ApplicationContext context,
        CorsUtils corsUtils,
        ReactiveDiscoveryClient discoveryClient,
        List<FilterDefinition> commonFilters,
        List<SchemeHandler> schemeHandlersList,
        List<RouteDefinitionProducer> routeDefinitionProducers
    ) {
        this.context = context;
        this.corsUtils = corsUtils;
        this.discoveryClient = discoveryClient;
        this.commonFilters = commonFilters;
        this.routeDefinitionProducers = routeDefinitionProducers;

        for (SchemeHandler schemeHandler : schemeHandlersList) {
            schemeHandlers.put(schemeHandler.getAuthenticationScheme(), schemeHandler);
        }
    }

    Flux<List<ServiceInstance>> getServiceInstances() {
        return discoveryClient.getServices()
            .flatMap(service -> discoveryClient.getInstances(service)
            .collectList());
    }

    void setAuth(RouteDefinition routeDefinition, Authentication auth) {
        if (auth != null && auth.getScheme() != null) {
            SchemeHandler schemeHandler = schemeHandlers.get(auth.getScheme());
            if (schemeHandler != null) {
                schemeHandler.apply(routeDefinition, auth);
            }
        }
    }

    void setCors(ServiceInstance serviceInstance) {
        corsUtils.setCorsConfiguration(
            serviceInstance.getServiceId().toLowerCase(),
            serviceInstance.getMetadata(),
            (prefix, serviceId, config) -> {
                serviceId = serviceInstance.getMetadata().getOrDefault(APIML_ID, serviceInstance.getServiceId().toLowerCase());
                getCorsConfigurationSource().registerCorsConfiguration("/" + serviceId + "/**", config);
            });
    }

    Stream<RoutedService> getRoutedService(ServiceInstance serviceInstance) {
        // FIXME: this is till the SCGW and GW uses the same DS. The rouing rules should be different for each application
        if (org.apache.commons.lang.StringUtils.equalsIgnoreCase("GATEWAY", serviceInstance.getServiceId())) {
            return Stream.of(new RoutedService("zuul", "", "/"));
        }

        return metadataParser.parseToListRoute(serviceInstance.getMetadata()).stream()
            // sorting avoid a conflict with the more general pattern
            .sorted(Comparator.<RoutedService>comparingInt(x -> StringUtils.removeFirstAndLastOccurrence(x.getGatewayUrl(), "/").length()).reversed());
    }

    /**
     * It generates each rule for each combination of instance x routing x generator ({@link RouteDefinitionProducer})
     * The routes are sorted by serviceUrl to avoid clashing between multiple levels of paths, ie. / vs. /a.
     * Sorting routes and generators by order allows to redefine order of each rule. There is no possible to have
     * multiple valid rules for the same case at one moment.
     *
     * @return routing rules
     */
    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        AtomicInteger order = new AtomicInteger();
        // iterate over services
        return getServiceInstances().flatMap(Flux::fromIterable).map(serviceInstance -> {
            Authentication auth = metadataParser.parseAuthentication(serviceInstance.getMetadata());
            // configure CORS for the service (if necessary)
            setCors(serviceInstance);
            // iterate over routing definition (ordered from the longest one to match with the most specific)
            return getRoutedService(serviceInstance)
                .map(routedService ->
                    routeDefinitionProducers.stream()
                    .sorted(Comparator.comparingInt(x -> x.getOrder()))
                    .map(rdp -> {
                        // generate a new routing rule by a specific produces
                        RouteDefinition routeDefinition = rdp.get(serviceInstance, routedService);
                        routeDefinition.setOrder(order.getAndIncrement());
                        routeDefinition.getFilters().addAll(commonFilters);
                        setAuth(routeDefinition, auth);

                        return routeDefinition;
                    }).collect(Collectors.toList())
            ).collect(Collectors.toList());
        })
        .flatMapIterable(list -> list)
        .flatMapIterable(list -> list);
    }

}
