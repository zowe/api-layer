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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;
import reactor.core.publisher.Mono;

import java.net.URI;

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

    private TransformService transformService;

    public PageRedirectionFilterFactory(GatewayClient gatewayClient) {
        super(Config.class);
        this.transformService = new TransformService(gatewayClient);
    }

    private String getNewLocationUrl(Config config, String location) {
        if (location == null) {
            return "";
        }

        try {
            URI locationUri = URI.create(location);
            // remove scheme, host, and port
            URI pathUri = UriComponentsBuilder.fromPath(locationUri.getPath()).query(locationUri.getQuery()).build().toUri();
            String newUrl = transformService.transformURL(StringUtils.toRootLowerCase(config.serviceId), pathUri.toString(), config.getRoutedService(), false, locationUri);
            if (isAttlsEnabled) {
                newUrl = UriComponentsBuilder.fromUriString(newUrl).scheme("https").build().toUriString();
            }
            return newUrl;
        } catch (URLTransformationException e) {
            log.debug("The URL for the redirect {} cannot be transformed: {}", location, e.getMessage());
            return "";
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange)
            .then(processNewLocationUrl(exchange, config));
    }

    private Mono<Void> processNewLocationUrl(ServerWebExchange exchange, Config config) {
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

        private String gatewayUrl;
        private String serviceUrl;

        RoutedService getRoutedService() {
            return new RoutedService("used", gatewayUrl, serviceUrl);
        }

    }

}
