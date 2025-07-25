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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PageRedirectionFilterFactory is a Spring Cloud Gateway Filter Factory that adapts a response from a routed service
 * to handle 3xx status codes, and applies the headers to the response object.
 *
 */
@Component
@Slf4j
public class PageRedirectionFilterFactory extends AbstractGatewayFilterFactory<PageRedirectionFilterFactory.Config> {

    private static final String SLASH = "/";

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private static final EurekaMetadataParser EUREKA_METADATA_PARSER = new EurekaMetadataParser();

    private final TransformService transformService;
    private final DiscoveryClient discoveryClient;

    public PageRedirectionFilterFactory(
        GatewayClient gatewayClient,
        DiscoveryClient discoveryClient
    ) {
        super(Config.class);
        this.transformService = new TransformService(gatewayClient);
        this.discoveryClient = discoveryClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        Optional<ServiceInstance> instance = discoveryClient.getInstances(config.serviceId).stream()
            .filter(i -> config.getInstanceId().equalsIgnoreCase(i.getInstanceId()))
            .findFirst();
        return (exchange, chain) -> chain.filter(exchange)
            .then(Mono.defer(() -> processNewLocationUrl(exchange, config, instance)));
    }

    private URI getHostUri(ServiceInstance instance) {
        return UriComponentsBuilder.newInstance()
            .host(instance.getHost())
            .port(instance.getPort())
            .build().toUri();
    }

    private URI getHostUri(URI uri) {
        return UriComponentsBuilder.newInstance()
            .host(uri.getHost())
            .port(uri.getPort())
            .build().toUri();
    }

    private Optional<ServiceInstance> getInstance(URI locationUri, Optional<ServiceInstance> instance) {
        if (locationUri.getHost() == null) {
            return instance;
        }

        URI hostUri = getHostUri(locationUri);
        if (instance.map(i -> getHostUri(i).equals(hostUri)).orElse(false)) {
            return instance;
        }

        return discoveryClient.getServices().stream()
            .map(discoveryClient::getInstances)
            .flatMap(List::stream)
            .filter(i -> hostUri.equals(getHostUri(i)))
            .findFirst();
    }

    private String normalizePath(String path) {
        if (!path.startsWith(SLASH)) {
            path = SLASH + path;
        }
        if (!path.endsWith(SLASH)) {
            path = path + SLASH;
        }
        return path;
    }

    private boolean isMatching(RoutedService route, URI uri) {
        var servicePath = normalizePath(route.getServiceUrl());
        var locationPath = normalizePath(uri.getPath());
        return locationPath.startsWith(servicePath);
    }

    private Mono<Void> processNewLocationUrl(ServerWebExchange exchange, Config config, Optional<ServiceInstance> instance) {
        var response = exchange.getResponse();
        var isRedirect = Optional.ofNullable(response.getStatusCode()).map(HttpStatusCode::is3xxRedirection).orElse(false);
        if (!isRedirect) {
            return Mono.empty();
        }

        var location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (StringUtils.isBlank(location)) {
            return Mono.empty();
        }

        var locationUri = URI.create(location);
        var targetInstance = getInstance(locationUri, instance);
        var defaultRoute = config.getRoutedService();

        AtomicReference<String> newUrl = new AtomicReference<>();
        if (targetInstance == instance && isMatching(defaultRoute, locationUri)) {
            // try the preferable route on the same instance (the same as in the original request)
            try {
                newUrl.set(transformService.transformURL(
                    StringUtils.toRootLowerCase(config.serviceId),
                    UriComponentsBuilder.fromPath(locationUri.getPath()).query(locationUri.getQuery()).build().toUri().toString(),
                    defaultRoute,
                    false,
                    locationUri
                ));
            } catch (URLTransformationException e) {
                log.debug("Cannot transform URL on the same route", e);
                return Mono.empty();
            }
        }

        if (newUrl.get() == null) {
            // try to find a matching routing for the service instance
            targetInstance.ifPresent(i -> {
                var routes = EUREKA_METADATA_PARSER.parseRoutes(i.getMetadata());

                try {
                    newUrl.set(transformService.transformURL(
                        ServiceType.ALL,
                        StringUtils.toRootLowerCase(config.serviceId),
                        location,
                        routes,
                        false
                    ));
                } catch (URLTransformationException e) {
                    log.debug("Cannot transform URL", e);
                }
            });
        }

        if (newUrl.get() != null) {
            // if the new URL was defined, decorate (scheme by AT-TLS) and set
            if (isAttlsEnabled) {
                newUrl.set(UriComponentsBuilder.fromUriString(newUrl.get()).scheme("https").build().toUriString());
            }

            exchange.getResponse().getHeaders().set(HttpHeaders.LOCATION, newUrl.toString());
        }

        // in case url was not transformed leave it as it is (routing could be outside the Zowe)
        return Mono.empty();
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
