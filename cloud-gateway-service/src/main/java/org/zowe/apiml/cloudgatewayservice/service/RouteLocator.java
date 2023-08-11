/*
 * Copyright (c) 2022 Broadcom.  All Rights Reserved.  The term
 * "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This software and all information contained therein is
 * confidential and proprietary and shall not be duplicated,
 * used, disclosed, or disseminated in any way except as
 * authorized by the applicable license agreement, without the
 * express written permission of Broadcom.  All authorized
 * reproductions must be marked with this language.
 *
 * EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO
 * THE EXTENT PERMITTED BY APPLICABLE LAW, BROADCOM PROVIDES THIS
 * SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT
 * LIMITATION, ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL BROADCOM
 * BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR
 * DAMAGE, DIRECT OR INDIRECT, FROM THE USE OF THIS SOFTWARE,
 * INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS
 * INTERRUPTION, GOODWILL, OR LOST DATA, EVEN IF BROADCOM IS
 * EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
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
import org.zowe.apiml.cloudgatewayservice.service.routing.RouteDefinitionProducer;
import org.zowe.apiml.cloudgatewayservice.service.scheme.SchemeHandler;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.util.CorsUtils;
import org.zowe.apiml.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

@Service
public class RouteLocator implements RouteDefinitionLocator {

    private final ApplicationContext context;

    private final CorsUtils corsUtils;
    private final ReactiveDiscoveryClient discoveryClient;

    private final List<FilterDefinition> commonFilters;
    private final List<RouteDefinitionProducer> routeDefinitionProducers;
    private final Map<String, SchemeHandler> schemeHandlers = new HashMap<>();

    @Getter(lazy=true, value = AccessLevel.PRIVATE)
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
            schemeHandlers.put(schemeHandler.getAuthenticationScheme().getScheme(), schemeHandler);
        }
    }

    private Flux<List<ServiceInstance>> getServiceInstances() {
        return discoveryClient.getServices()
            .flatMap(service -> discoveryClient.getInstances(service)
            .collectList());
    }

    protected void setAuth(RouteDefinition routeDefinition, Authentication auth) {
        if (auth != null && auth.getScheme() != null) {
            SchemeHandler schemeHandler = schemeHandlers.get(auth.getScheme());
            if (schemeHandler != null) {
                schemeHandler.apply(routeDefinition, auth);
            }
        }
    }

    private void setCors(ServiceInstance serviceInstance) {
        corsUtils.setCorsConfiguration(
            serviceInstance.getServiceId().toLowerCase(),
            serviceInstance.getMetadata(),
            (prefix, serviceId, config) -> {
                serviceId = serviceInstance.getMetadata().getOrDefault(APIML_ID, serviceInstance.getServiceId());
                getCorsConfigurationSource().registerCorsConfiguration("/" + serviceId + "/**", config);
            });
    }

    private String normalizeUrl(String in) {
        in = in.replaceAll("\\*", "");
        in = StringUtils.removeFirstAndLastOccurrence(in, "/");
        return in;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        EurekaMetadataParser metadataParser = new EurekaMetadataParser();
        AtomicInteger order = new AtomicInteger();
        /**
         * It generates each rule for each combination of instance x routing x generator ({@link RouteDefinitionProducer})
         *
         * The routes are sorted by serviceUrl to avoid clashing between multiple levels of paths, ie. /** vs /a, stars are
         * removed in {@link #normalizeUrl(String)}.
         *
         * Sorting routes and generators by order allows to redefine order of each rule. There is no possible to have
         * multiple valid rules for the same case at one moment.
         */
        return getServiceInstances().flatMap(Flux::fromIterable).map(serviceInstance -> {
            Authentication auth = metadataParser.parseAuthentication(serviceInstance.getMetadata());
            setCors(serviceInstance);
            return metadataParser.parseToListRoute(serviceInstance.getMetadata()).stream()
                // sorting avoid a conflict with the more general pattern
                .sorted(Comparator.<RoutedService>comparingInt(x -> normalizeUrl(x.getServiceUrl()).length()).reversed())
                .map(routedService ->
                    routeDefinitionProducers.stream()
                    .sorted(Comparator.comparingInt(x -> x.getOrder()))
                    .map(rdp -> {
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
