/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.service;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
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
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.gateway.service.routing.RouteDefinitionProducer;
import org.zowe.apiml.gateway.service.scheme.SchemeHandler;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.util.CorsUtils;
import org.zowe.apiml.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING;

@Service
public class RouteLocator implements RouteDefinitionLocator {

    private static final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    @Value("${apiml.service.forwardClientCertEnabled:false}")
    private boolean forwardingClientCertEnabled;

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

    void setAuth(ServiceInstance serviceInstance, RouteDefinition routeDefinition, Authentication auth) {
        if (auth != null && auth.getScheme() != null) {
            SchemeHandler schemeHandler = schemeHandlers.get(auth.getScheme());
            if (schemeHandler != null) {
                schemeHandler.apply(serviceInstance, routeDefinition, auth);
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
        return metadataParser.parseToListRoute(serviceInstance.getMetadata()).stream()
            // sorting avoid a conflict with the more general pattern
            .sorted(Comparator.<RoutedService>comparingInt(x -> StringUtils.removeFirstAndLastOccurrence(x.getGatewayUrl(), "/").length()).reversed());
    }

    static <T> List<T> join(List<T> a, List<T> b) {
        if (b.isEmpty()) return a;

        List<T> output = new LinkedList<>(a);
        output.addAll(b);
        return output;
    }

    List<FilterDefinition> getPostRoutingFilters(ServiceInstance serviceInstance) {
        List<FilterDefinition> serviceRelated = new LinkedList<>();
        if (forwardingClientCertEnabled
                && Optional.ofNullable(serviceInstance.getMetadata().get(SERVICE_SUPPORTING_CLIENT_CERT_FORWARDING))
                    .map(Boolean::parseBoolean)
                    .orElse(false)
        ) {
            FilterDefinition forwardClientCertFilter = new FilterDefinition();
            forwardClientCertFilter.setName("ForwardClientCertFilterFactory");
            serviceRelated.add(forwardClientCertFilter);
        }

        FilterDefinition pageRedirectionFilter = new FilterDefinition();
        pageRedirectionFilter.setName("PageRedirectionFilterFactory");
        pageRedirectionFilter.addArg("serviceId", serviceInstance.getServiceId());
        pageRedirectionFilter.addArg("instanceId", serviceInstance.getInstanceId());
        serviceRelated.add(pageRedirectionFilter);

        return join(commonFilters, serviceRelated);
    }

    private List<RouteDefinition> getAuthFilterPerRoute(
        AtomicInteger orderHolder,
        ServiceInstance serviceInstance,
        List<FilterDefinition> postRoutingFilters
    ) {
        Authentication auth = metadataParser.parseAuthentication(serviceInstance.getMetadata());
        // iterate over routing definition (ordered from the longest one to match with the most specific)
        return getRoutedService(serviceInstance)
            .map(routedService ->
                routeDefinitionProducers.stream()
                    .sorted(Comparator.comparingInt(RouteDefinitionProducer::getOrder))
                    .map(rdp -> {
                        // generate a new routing rule by a specific produces
                        RouteDefinition routeDefinition = rdp.get(serviceInstance, routedService);
                        routeDefinition.setOrder(orderHolder.getAndIncrement());
                        routeDefinition.getFilters().addAll(postRoutingFilters);
                        setAuth(serviceInstance, routeDefinition, auth);

                        return routeDefinition;
                    }).collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .collect(Collectors.toList());
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
        // counter of generated route definition to prevent clashing by the order
        AtomicInteger order = new AtomicInteger();
        // iterate over services
        return getServiceInstances().flatMap(Flux::fromIterable).map(serviceInstance -> {
            // configure CORS for the service (if necessary)
            setCors(serviceInstance);

            // generate route definition per services and its routing rules
            return getAuthFilterPerRoute(order, serviceInstance, getPostRoutingFilters(serviceInstance));
        })
        .flatMapIterable(list -> list);
    }

}
