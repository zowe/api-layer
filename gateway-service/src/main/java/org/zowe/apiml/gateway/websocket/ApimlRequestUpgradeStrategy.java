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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.adapter.ContextWebSocketHandler;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketHandlerAdapter;
import org.springframework.web.reactive.socket.server.upgrade.StandardWebSocketUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.zowe.apiml.gateway.config.ApimlServerEndpointConfig;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

import static reactor.core.publisher.Mono.*;

public class ApimlRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

    private final HttpClientProperties httpClientProperties;
    private final WebSocketProperties properties;

    public ApimlRequestUpgradeStrategy(HttpClientProperties httpClientProperties) {
        this(httpClientProperties, new WebSocketProperties());
    }

    public ApimlRequestUpgradeStrategy(HttpClientProperties httpClientProperties, WebSocketProperties properties) {
        this.httpClientProperties = httpClientProperties;
        this.properties = properties;
    }

    @Override
    protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
                                          ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

        getContainer(request).upgradeHttpToWebSocket(request, response, endpointConfig, pathParams);
    }

    @Override
    public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
                              @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {
        if (handler instanceof BackendWebSocketHandler backend) {
            if (!validClientHandshake(exchange)) {
                backend.abort();
                return exchange.getResponse().setComplete();
            }
            // Let the service choose before sending the 101 response.
            return backend.connect()
                .timeout(Duration.ofMillis(properties.getConnectTimeout()),
                    Mono.error(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "WebSocket backend connection timed out")))
                .flatMap(protocol -> {
                    if (!protocol.isEmpty() && !handler.getSubProtocols().contains(protocol)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Backend selected an unoffered WebSocket subprotocol"));
                    }
                    String selected = protocol.isEmpty() ? null : protocol;
                    HandshakeInfo original = handshakeInfoFactory.get();
                    var info = new HandshakeInfo(original.getUri(), original.getHeaders(), original.getCookies(),
                        original.getPrincipal(), selected, original.getRemoteAddress(), original.getAttributes(), original.getLogPrefix());
                    return upgradeClient(exchange, handler, selected, info);
                })
                .doOnError(error -> backend.abort())
                .doOnCancel(backend::abort);
        }
        return upgradeClient(exchange, handler, subProtocol, handshakeInfoFactory.get());
    }

    private boolean validClientHandshake(ServerWebExchange exchange) {
        var headers = exchange.getRequest().getHeaders();
        boolean supportedVersion = headers.getOrEmpty("Sec-WebSocket-Version").stream()
            .flatMap(value -> Arrays.stream(value.split(","))).anyMatch(value -> "13".equals(value.trim()));
        if (!supportedVersion) {
            exchange.getResponse().setStatusCode(HttpStatus.UPGRADE_REQUIRED);
            exchange.getResponse().getHeaders().set("Sec-WebSocket-Version", "13");
            return false;
        }
        String key = headers.getFirst("Sec-WebSocket-Key");
        try {
            if (key != null && key.length() == 24 && Base64.getDecoder().decode(key).length == 16) return true;
        } catch (IllegalArgumentException ignored) {
            // Tomcat applies the same validation during the native upgrade.
        }
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        return false;
    }

    private Mono<Void> upgradeClient(ServerWebExchange exchange, WebSocketHandler handler,
                                     @Nullable String subProtocol, HandshakeInfo handshakeInfo) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
        HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);

        DataBufferFactory bufferFactory = response.bufferFactory();

        // Trigger WebFlux preCommit actions and upgrade
        return exchange.getResponse().setComplete()
            .then(deferContextual(contextView -> {
                Endpoint endpoint = handler instanceof PartialWebSocketHandler partial ? partial.relay().clientEndpoint() : new StandardWebSocketHandlerAdapter(
                    ContextWebSocketHandler.decorate(handler, contextView),
                    session -> {
                        properties.configure(session);
                        //Set the gateway outbound frame limit for websockets
                        if (httpClientProperties.getWebsocket().getMaxFramePayloadLength() != null) {
                            session.setMaxTextMessageBufferSize(httpClientProperties.getWebsocket().getMaxFramePayloadLength());
                            session.setMaxBinaryMessageBufferSize(httpClientProperties.getWebsocket().getMaxFramePayloadLength());
                        }
                        return new ApimlWebSocketSession(session, handshakeInfo, bufferFactory);
                    });
                String requestURI = servletRequest.getRequestURI();
                var config = new ApimlServerEndpointConfig(requestURI, endpoint);
                config.setCompressionEnabled(!(handler instanceof PartialWebSocketHandler));
                config.setSubprotocols(subProtocol != null ?
                    Collections.singletonList(subProtocol) : Collections.emptyList());

                try {
                    upgradeHttpToWebSocket(servletRequest, servletResponse, config, Collections.emptyMap());
                } catch (Exception ex) {
                    return error(ex);
                }
                // Tomcat can reject the handshake with an HTTP error and return without opening an endpoint.
                if (handler instanceof BackendWebSocketHandler backend
                    && servletResponse.getStatus() != HttpServletResponse.SC_SWITCHING_PROTOCOLS) {
                    backend.abort();
                }
                // Complete the HTTP exchange so Tomcat can flush the upgrade and dispatch WebSocket reads.
                // The native endpoints own the relay's lifetime after the upgrade.
                return empty();
            }));
    }

}
