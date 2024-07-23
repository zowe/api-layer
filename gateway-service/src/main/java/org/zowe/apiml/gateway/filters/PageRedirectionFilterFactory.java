/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Stream;

import static reactor.core.publisher.Mono.empty;

/**
 * PageRedirectionFilterFactory is a Spring Cloud Gateway Filter Factory that adapts a response from a routed service
 * to handle 3xx status codes, and applies the headers to the response object.
 *
 */
@Component
@Slf4j
public class PageRedirectionFilterFactory extends AbstractGatewayFilterFactory<PageRedirectionFilterFactory.Config> {

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private final EurekaClient eurekaClient;
    private final EurekaMetadataParser metadataParser;
    private TransformService transformService;

    public PageRedirectionFilterFactory(
            EurekaClient eurekaClient,
            @Qualifier("getEurekaMetadataParser") EurekaMetadataParser metadataParser,
            GatewayClient gatewayClient) {
        super(Config.class);
        this.eurekaClient = eurekaClient;
        this.metadataParser = metadataParser;
        this.transformService = new TransformService(gatewayClient);
    }

    private String getNewLocationUrl(Config config, String location) {
        if (location == null) {
            return "";
        }

        return ((Stream<?>) eurekaClient.getInstancesById(config.instanceId).stream())
            .findAny()
                .map(InstanceInfo.class::cast)
                .map(serviceInstance -> {
                        Map<String, String> metadata = serviceInstance.getMetadata();
                        RoutedServices routes = metadataParser.parseRoutes(metadata);
                        try {
                            String newUrl = transformService.transformURL(ServiceType.ALL, StringUtils.toRootLowerCase(config.serviceId), location, routes, false);
                            if (isAttlsEnabled) {
                                newUrl = UriComponentsBuilder.fromUriString(newUrl).scheme("https").build().toUriString();
                            }
                            return newUrl;
                        } catch (URLTransformationException e) {
                            log.debug("The URL for the redirect {} cannot be transformed: {}", location, e.getMessage());
                            return "";
                        }
                    }
                )
                .orElse("");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange)
            .then(processNewLocationUrl(exchange, chain, config));
    }

    private Mono<Void> processNewLocationUrl(ServerWebExchange exchange, GatewayFilterChain chain, Config config) {
        return Mono.fromCallable(() -> {
            var response = exchange.getResponse();
            if (response.getStatusCode().is3xxRedirection()) {
                return getNewLocationUrl(config, response.getHeaders().getFirst(HttpHeaders.LOCATION));
            }
            return "";
        }).flatMap(newUrl -> {
            if (StringUtils.isNotBlank(newUrl)) {
                exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, newUrl);
            }
            return empty();
        });
    }

    @Data
    public static class Config {

        private String serviceId;
        private String instanceId;

    }

}
