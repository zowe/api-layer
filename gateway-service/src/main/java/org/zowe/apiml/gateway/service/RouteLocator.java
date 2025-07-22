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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Service;
import org.springframework.util.PatternMatchUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.gateway.service.routing.RouteDefinitionProducer;
import org.zowe.apiml.gateway.service.scheme.SchemeHandler;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RouteLocator implements RouteDefinitionLocator {

    private static final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    @Value("${apiml.routing.ignoredServices:}")
    private String[] ignoredServices;

    @Value("${apiml.service.forwardClientCertEnabled:false}")
    private boolean forwardingClientCertEnabled;

    @Value("${apiml.gateway.servicesToLimitRequestRate:-}")
    List<String> servicesToLimitRequestRate;

    private final ReactiveDiscoveryClient discoveryClient;

    private final List<FilterDefinition> commonFilters;
    private final List<RouteDefinitionProducer> routeDefinitionProducers;
    private final List<SchemeHandler> schemeHandlersList;
    private final Map<AuthenticationScheme, SchemeHandler> schemeHandlers = new EnumMap<>(AuthenticationScheme.class);

    @PostConstruct
    void afterPropertiesSet() {
        for (SchemeHandler schemeHandler : schemeHandlersList) {
            schemeHandlers.put(schemeHandler.getAuthenticationScheme(), schemeHandler);
        }
    }

    Flux<List<ServiceInstance>> getServiceInstances() {
        return discoveryClient.getServices()
            .filter(this::filterIgnored)
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

    List<FilterDefinition> getPostRoutingFilters(ServiceInstance serviceInstance, RoutedService routedService) {
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
        //Allow encoded characters by default
        if (!Optional.ofNullable(serviceInstance.getMetadata().get(ENABLE_URL_ENCODED_CHARACTERS))
            .map(Boolean::parseBoolean)
            .orElse(true)) {
            FilterDefinition forbidEncodedCharactersFilter = new FilterDefinition();
            forbidEncodedCharactersFilter.setName("ForbidEncodedCharactersFilterFactory");
            serviceRelated.add(forbidEncodedCharactersFilter);
        }

        if (Optional.ofNullable(serviceInstance.getMetadata().get(APPLY_RATE_LIMITER_FILTER))
            .map(Boolean::parseBoolean)
            .orElse(false)) {
            FilterDefinition rateLimiterFilter = new FilterDefinition();
            rateLimiterFilter.setName("InMemoryRateLimiterFilterFactory");
            rateLimiterFilter.addArg("capacity", serviceInstance.getMetadata().get("apiml.gateway.rateLimiterCapacity"));
            rateLimiterFilter.addArg("tokens", serviceInstance.getMetadata().get("apiml.gateway.rateLimiterTokens"));
            rateLimiterFilter.addArg("refillDuration", serviceInstance.getMetadata().get("apiml.gateway.rateLimiterRefillDuration"));
            serviceRelated.add(rateLimiterFilter);
        } else if (servicesToLimitRequestRate != null && servicesToLimitRequestRate.contains(serviceInstance.getServiceId().toLowerCase())) {
            FilterDefinition rateLimiterFilter = new FilterDefinition();
            rateLimiterFilter.setName("InMemoryRateLimiterFilterFactory");
            serviceRelated.add(rateLimiterFilter);
        }

        FilterDefinition pageRedirectionFilter = new FilterDefinition();
        pageRedirectionFilter.setName("PageRedirectionFilterFactory");
        pageRedirectionFilter.addArg("serviceId", serviceInstance.getServiceId());
        pageRedirectionFilter.addArg("instanceId", serviceInstance.getInstanceId());
        pageRedirectionFilter.addArg("gatewayUrl", routedService.getGatewayUrl());
        pageRedirectionFilter.addArg("serviceUrl", routedService.getServiceUrl());
        serviceRelated.add(pageRedirectionFilter);

        return join(commonFilters, serviceRelated);
    }

    private List<RouteDefinition> getAuthFilterPerRoute(
        AtomicInteger orderHolder,
        ServiceInstance serviceInstance
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
                        routeDefinition.getFilters().addAll(getPostRoutingFilters(serviceInstance, routedService));
                        setAuth(serviceInstance, routeDefinition, auth);

                        return routeDefinition;
                    }).toList()
            )
            .flatMap(List::stream)
            .toList();
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
        return getServiceInstances().flatMap(Flux::fromIterable).map(serviceInstance ->
            // generate route definition per services and its routing rules
            getAuthFilterPerRoute(order, serviceInstance)
        )
        .flatMapIterable(list -> list);
    }

    private boolean filterIgnored(String serviceId) {
        return !PatternMatchUtils.simpleMatch(ignoredServices, serviceId.toLowerCase());
    }

}
