/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.zowe.apiml.product.web.HttpConfig;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@RequiredArgsConstructor
public class PartialWebSocketRoutingFilter implements GlobalFilter, Ordered {
    public static final String MODE_METADATA = "apiml.websocket.partialForwarding";
    private final WebSocketProperties properties;
    private final HttpClientProperties httpProperties;
    private final WebSocketRelayResources resources;
    private final WebSocketHandshakePolicy policy;
    private final WsWebSocketContainer container;
    private final HttpConfig httpConfig;
    private final WebSocketService service;
    private final ObjectProvider<List<HttpHeadersFilter>> headerFilters;

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        if (route == null || url == null || isAlreadyRouted(exchange) || !properties.getPartial().isEnabled()
            || !Boolean.parseBoolean(String.valueOf(route.getMetadata().get(MODE_METADATA)))) {
            return chain.filter(exchange);
        }
        String scheme = url.getScheme().toLowerCase(Locale.ROOT);
        if ("websocket".equalsIgnoreCase(exchange.getRequest().getHeaders().getUpgrade())
            && ("http".equals(scheme) || "https".equals(scheme))) {
            url = UriComponentsBuilder.fromUri(url).scheme("https".equals(scheme) ? "wss" : "ws").build(true).toUri();
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, url);
            scheme = url.getScheme();
        }
        if (!"ws".equals(scheme) && !"wss".equals(scheme)) {
            return chain.filter(exchange);
        }
        setAlreadyRouted(exchange);
        var headers = filteredHeaders(exchange);
        var protocols = exchange.getRequest().getHeaders().getOrEmpty("Sec-WebSocket-Protocol").stream()
            .flatMap(value -> Arrays.stream(StringUtils.tokenizeToStringArray(value, ","))).toList();
        Integer legacyLimit = httpProperties.getWebsocket().getMaxFramePayloadLength();
        PartialWebSocketRelay relay;
        try {
            relay = new PartialWebSocketRelay(properties, resources, policy, container, httpConfig.secureSslContext(),
                url, headers, protocols, route.getUri().getHost(), legacyLimit == null ? 8192 : legacyLimit);
        } catch (RejectedExecutionException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage()));
        }
        return Mono.defer(() -> service.handleRequest(exchange, new PartialWebSocketHandler(relay, protocols)))
            .doOnError(error -> relay.fail("handshake", "WebSocket handshake failed"))
            .doOnCancel(() -> relay.fail("cancelled", "WebSocket handshake cancelled"));
    }

    private HttpHeaders filteredHeaders(ServerWebExchange exchange) {
        var filtered = HttpHeadersFilter.filterRequest(headerFilters.getIfAvailable(ArrayList::new), exchange);
        var result = new HttpHeaders();
        filtered.forEach((name, values) -> {
            if (!name.toLowerCase(Locale.ROOT).startsWith("sec-websocket") && !HttpHeaders.HOST.equalsIgnoreCase(name)
                && !HttpHeaders.CONNECTION.equalsIgnoreCase(name) && !HttpHeaders.UPGRADE.equalsIgnoreCase(name)) {
                result.put(name, List.copyOf(values));
            }
        });
        if (exchange.getAttributeOrDefault(PRESERVE_HOST_HEADER_ATTRIBUTE, false)) {
            result.put(HttpHeaders.HOST, exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.HOST));
        }
        return result;
    }
}
