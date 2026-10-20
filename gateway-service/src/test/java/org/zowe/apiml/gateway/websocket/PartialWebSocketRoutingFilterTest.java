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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.RemoteEndpoint;
import org.apache.tomcat.websocket.WsSession;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.zowe.apiml.product.web.HttpConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

class PartialWebSocketRoutingFilterTest {
    private final WebSocketProperties properties = new WebSocketProperties();
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final WebSocketRelayResources resources = new WebSocketRelayResources(properties, meters);
    private final WsWebSocketContainer container = mock(WsWebSocketContainer.class);
    private final WebSocketService service = mock(WebSocketService.class);
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @AfterEach
    void cleanup() {
        resources.close();
    }

    @SuppressWarnings("unchecked")
    private PartialWebSocketRoutingFilter filter() {
        ObjectProvider<List<HttpHeadersFilter>> filters = mock(ObjectProvider.class);
        when(filters.getIfAvailable(any())).thenReturn(List.of((HttpHeadersFilter) (headers, exchange) -> {
            var filtered = new HttpHeaders();
            filtered.addAll(headers);
            filtered.remove("X-Remove");
            filtered.set("X-Filtered", "applied");
            return filtered;
        }));
        return new PartialWebSocketRoutingFilter(properties, new HttpClientProperties(), resources,
            new WebSocketHandshakePolicy(), container, mock(HttpConfig.class), service, filters);
    }

    private MockServerWebExchange exchange(boolean optIn) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://public.example/service/ws")
            .header("Upgrade", "websocket").header("Connection", "Upgrade")
            .header("Host", "public.example").header("Authorization", "test-credential")
            .header("Sec-WebSocket-Protocol", "first, second").header("Sec-WebSocket-Key", "old-key")
            .header("Sec-WebSocket-Extensions", "permessage-deflate").header("X-Remove", "private"));
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, Route.async().id("service").uri("lb://service")
            .metadata(PartialWebSocketRoutingFilter.MODE_METADATA, Boolean.toString(optIn))
            .predicate(ignored -> true).build());
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, URI.create("https://backend.example:1234/encoded%20path?q=a%2Fb"));
        return exchange;
    }

    @ParameterizedTest
    @CsvSource({"false,true,false", "true,false,false", "true,true,true"})
    void leavesOtherRoutesWithTheExistingChain(boolean enabled, boolean optIn, boolean alreadyRouted) {
        properties.getPartial().setEnabled(enabled);
        var exchange = exchange(optIn);
        if (alreadyRouted) setAlreadyRouted(exchange);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();
        verify(chain).filter(exchange);
        verifyNoInteractions(service, container);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void usesTheResolvedUrlFilteredCredentialsAndOriginalProtocolOffer(boolean preserveHost) throws Exception {
        properties.getPartial().setEnabled(true);
        var exchange = exchange(true);
        exchange.getAttributes().put(PRESERVE_HOST_HEADER_ATTRIBUTE, preserveHost);
        var client = mock(WsSession.class);
        var backend = mock(WsSession.class);
        for (var session : List.of(client, backend)) {
            when(session.getAsyncRemote()).thenReturn(mock(RemoteEndpoint.Async.class));
            when(session.getUserProperties()).thenReturn(new HashMap<>());
            when(session.getNegotiatedSubprotocol()).thenReturn("first");
        }
        when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class))).thenAnswer(call -> {
            call.<Endpoint>getArgument(0).onOpen(backend, call.getArgument(1));
            return backend;
        });
        when(service.handleRequest(eq(exchange), any())).thenAnswer(call -> {
            var handler = (PartialWebSocketHandler) call.<WebSocketHandler>getArgument(1);
            assertThat(handler.getSubProtocols()).containsExactly("first", "second");
            return handler.connect().doOnNext(protocol -> {
                assertThat(protocol).isEqualTo("first");
                handler.relay().clientEndpoint().onOpen(client, mock(EndpointConfig.class));
            }).then();
        });
        StepVerifier.create(filter().filter(exchange, chain)).verifyComplete();
        var config = org.mockito.ArgumentCaptor.forClass(ClientEndpointConfig.class);
        verify(container).connectToServer(any(Endpoint.class), config.capture(),
            eq(URI.create("wss://backend.example:1234/encoded%20path?q=a%2Fb")));
        assertThat(config.getValue().getPreferredSubprotocols()).containsExactly("first", "second");
        var headers = new HashMap<String, List<String>>();
        config.getValue().getConfigurator().beforeRequest(headers);
        assertThat(headers).containsEntry("Authorization", List.of("test-credential"))
            .containsEntry("X-Filtered", List.of("applied"));
        assertThat(headers).doesNotContainKeys("X-Remove", "Sec-WebSocket-Key", "Sec-WebSocket-Protocol", "Sec-WebSocket-Extensions", "Connection", "Upgrade");
        assertThat(headers.containsKey("Host")).isEqualTo(preserveHost);
        assertThat(isAlreadyRouted(exchange)).isTrue();
        verifyNoInteractions(chain);
    }

    @Test
    void synchronousHandshakeFailureReleasesTheConnectionSlot() {
        properties.getPartial().setEnabled(true);
        var exchange = exchange(true);
        when(service.handleRequest(eq(exchange), any())).thenThrow(new IllegalArgumentException("invalid handshake"));
        StepVerifier.create(filter().filter(exchange, chain)).expectError(IllegalArgumentException.class).verify();
        await().untilAsserted(() -> assertThat(meters.get("apiml.websocket.active").gauge().value()).isZero());
    }
}
