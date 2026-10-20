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
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.AbstractServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApimlRequestUpgradeStrategyTest {

    @Test
    void unofferedBackendProtocolFailsTheHttpHandshake() {
        var handler = mock(BackendWebSocketHandler.class);
        when(handler.connect()).thenReturn(Mono.just("unoffered"));
        when(handler.getSubProtocols()).thenReturn(List.of("offered"));

        StepVerifier.create(new ApimlRequestUpgradeStrategy(new HttpClientProperties())
            .upgrade(validHandshake(), handler, "offered", () -> null))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ResponseStatusException.class);
                assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(error).hasMessageContaining("unoffered WebSocket subprotocol");
            }).verify(Duration.ofSeconds(1));
        verify(handler).abort();
    }

    @Test
    void backendTimeoutFailsTheHttpHandshake() {
        var handler = mock(BackendWebSocketHandler.class);
        when(handler.connect()).thenReturn(Mono.never());
        var properties = new WebSocketProperties();
        properties.setConnectTimeout(10);

        StepVerifier.create(new ApimlRequestUpgradeStrategy(new HttpClientProperties(), properties)
            .upgrade(validHandshake(), handler, null, () -> null))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ResponseStatusException.class);
                assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
            }).verify(Duration.ofSeconds(1));
        verify(handler).abort();
    }

    private MockServerWebExchange validHandshake() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/websocket")
            .header("Sec-WebSocket-Version", "13")
            .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ=="));
    }

    @Test
    void givenHttpRequest_thenUpgrade() throws IOException, DeploymentException {
        var servletRequest = mock(AbstractServerHttpRequest.class);

        var resp = mock(AbstractServerHttpResponse.class);
        when(resp.setComplete()).thenReturn(Mono.empty());
        var exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(servletRequest);
        when(exchange.getResponse()).thenReturn(resp);

        var nativeReq = mock(HttpServletRequest.class);
        var nativeResp = mock(HttpServletResponse.class);

        when(nativeReq.getRequestURI()).thenReturn("/websocket");
        when(servletRequest.getNativeRequest()).thenReturn(nativeReq);
        when(resp.getNativeResponse()).thenReturn(nativeResp);
        var handler = mock(WebSocketHandler.class);
        var handShakeInfo = mock(HandshakeInfo.class);
        var updateStrategy = new ApimlRequestUpgradeStrategy(new HttpClientProperties());
        var serverContainer = mock(ServerContainer.class);
        ReflectionTestUtils.setField(updateStrategy, "serverContainer", serverContainer);

        StepVerifier.create(updateStrategy.upgrade(exchange, handler, null, () -> handShakeInfo)).expectComplete().verify();
        verify(serverContainer, times(1)).upgradeHttpToWebSocket(any(), any(), any(), any());
    }
}
