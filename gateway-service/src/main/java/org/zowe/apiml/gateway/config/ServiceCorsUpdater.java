/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;
import org.zowe.apiml.util.CorsUtils;
import reactor.core.publisher.Flux;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;

@Component
@RequiredArgsConstructor
public class ServiceCorsUpdater {

    private final CorsUtils corsUtils;
    private final ReactiveDiscoveryClient discoveryClient;
    private final RoutePredicateHandlerMapping handlerMapping;
    private final GlobalCorsProperties globalCorsProperties;

    @Getter
    private UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource;

    @PostConstruct
    void initCorsConfigurationSource() {
        urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource(new PathPatternParser());
        urlBasedCorsConfigurationSource.setCorsConfigurations(globalCorsProperties.getCorsConfigurations());
        corsUtils.registerDefaultCorsConfiguration(urlBasedCorsConfigurationSource::registerCorsConfiguration);
        handlerMapping.setCorsConfigurationSource(urlBasedCorsConfigurationSource);
    }

    @EventListener(RefreshRoutesEvent.class)
    public void onRefreshRoutesEvent(RefreshRoutesEvent event) {
        discoveryClient.getServices()
            .flatMap(service -> discoveryClient.getInstances(service).collectList())
            .flatMap(Flux::fromIterable)
            .toIterable()
            .forEach(serviceInstance ->
                corsUtils.setCorsConfiguration(
                    serviceInstance.getServiceId().toLowerCase(),
                    serviceInstance.getMetadata(),
                    (prefix, serviceId, config) -> {
                        serviceId = serviceInstance.getMetadata().getOrDefault(APIML_ID, serviceInstance.getServiceId().toLowerCase());
                        urlBasedCorsConfigurationSource.registerCorsConfiguration("/" + serviceId + "/**", config);
                    }
                )
            );
    }

}
