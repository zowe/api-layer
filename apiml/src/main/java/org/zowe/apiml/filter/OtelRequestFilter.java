/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.product.opentelemetry.RoutingContext;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Component
@ConditionalOnProperty(value = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
public class OtelRequestFilter implements WebFilter, GlobalFilter, Ordered {

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.port:10010}")
    private int gatewayPort;

    @Value("${apiml.internal-discovery.port:10011}")
    private int discoveryPort;

    @Value("${server.attlsServer.enabled:false}")
    private boolean isServerAttlsEnabled;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private void setDefaults(ServerWebExchange exchange, RoutingContext otelContext) {
        String serviceId;

        int port = exchange.getRequest().getLocalAddress().getPort();
        if (port == discoveryPort) {
            serviceId = "discovery";
        } else {
            var paths = exchange.getRequest().getPath().elements();
            var firstPath = paths.size() > 1 ? paths.get(1).value() : "";
            serviceId = firstPath;

            switch (firstPath) {
                case "apicatalog":
                    break;
                case "cachingservice":
                    break;
                case "zaas":
                case "gateway":
                default:
                    serviceId = "gateway";
            }
        }

        otelContext
            .method(exchange.getRequest().getMethod())
            .scheme(isServerAttlsEnabled ? "https" : exchange.getRequest().getURI().getScheme())
            .path(exchange.getRequest().getURI().getPath())
            .serviceId(serviceId)
            .instanceId(String.format("%s:%s:%d", hostname, serviceId, port));
    }

    private Mono<Void> filterInternal(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> filter) {
        var otelContext = RoutingContext.of(exchange);

        setDefaults(exchange, otelContext);

        return filter.apply(exchange)
            .then(Mono.fromRunnable(() -> Optional.ofNullable(exchange.getResponse())
                .map(ServerHttpResponse::getStatusCode)
                .map(HttpStatusCode::value)
                .ifPresent(otelContext::responseCode)
            ))
            .then(Mono.fromRunnable(() -> RoutingContext.of(exchange).issue()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return filterInternal(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return filterInternal(exchange, chain::filter);
    }

}
