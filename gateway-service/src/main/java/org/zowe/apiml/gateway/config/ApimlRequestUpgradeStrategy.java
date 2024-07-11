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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.core.io.buffer.DataBufferFactory;
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
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class ApimlRequestUpgradeStrategy extends StandardWebSocketUpgradeStrategy {

    @Override
    protected void upgradeHttpToWebSocket(HttpServletRequest request, HttpServletResponse response,
                                          ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws Exception {

        getContainer(request).upgradeHttpToWebSocket(request, response, endpointConfig, pathParams);
    }

    @Override
    public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
                              @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        HttpServletRequest servletRequest = ServerHttpRequestDecorator.getNativeRequest(request);
        HttpServletResponse servletResponse = ServerHttpResponseDecorator.getNativeResponse(response);

        HandshakeInfo handshakeInfo = handshakeInfoFactory.get();
        DataBufferFactory bufferFactory = response.bufferFactory();

        // Trigger WebFlux preCommit actions and upgrade
        return exchange.getResponse().setComplete()
            .then(Mono.deferContextual(contextView -> {
                Endpoint endpoint = new StandardWebSocketHandlerAdapter(
                    ContextWebSocketHandler.decorate(handler, contextView),
                    session -> new ApimlWebSocketSession(session, handshakeInfo, bufferFactory));

                String requestURI = servletRequest.getRequestURI();
                var config = new ApimlServerEndpointConfig(requestURI, endpoint);
                config.setSubprotocols(subProtocol != null ?
                    Collections.singletonList(subProtocol) : Collections.emptyList());

                try {
                    upgradeHttpToWebSocket(servletRequest, servletResponse, config, Collections.emptyMap());
                } catch (Exception ex) {
                    return Mono.error(ex);
                }
                return Mono.empty();
            }));
    }

}
