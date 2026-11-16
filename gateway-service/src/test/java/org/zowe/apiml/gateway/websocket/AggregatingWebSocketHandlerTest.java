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

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AggregatingWebSocketHandlerTest {
    private final WebSocketClient client = mock(WebSocketClient.class);
    private final WebSocketSession backend = mock(WebSocketSession.class);
    private final AggregatingWebSocketHandler handler = new AggregatingWebSocketHandler(client,
        URI.create("ws://backend/stream"), new HttpHeaders(), List.of("first", "second"), "service", new WebSocketHandshakePolicy());

    @Test
    void abortedClientUpgradeClosesBackendAndCancelsItsSubscription() {
        var cancelled = new AtomicBoolean();
        when(backend.getHandshakeInfo()).thenReturn(new HandshakeInfo(URI.create("ws://backend/stream"),
            new HttpHeaders(), Mono.empty(), "second"));
        when(backend.close(any())).thenReturn(Mono.empty());
        when(client.execute(any(), any(), any())).thenAnswer(call ->
            call.<WebSocketHandler>getArgument(2).handle(backend).doOnCancel(() -> cancelled.set(true)));

        assertThat(handler.connect().block(Duration.ofSeconds(1))).isEqualTo("second");
        verify(backend, never()).receive();
        handler.abort();

        assertThat(cancelled).isTrue();
        verify(backend).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void backendOpeningAfterCancellationIsClosedWithoutForwarding() {
        var endpoint = new AtomicReference<WebSocketHandler>();
        var cancelled = new AtomicBoolean();
        when(client.execute(any(), any(), any())).thenAnswer(call -> {
            endpoint.set(call.getArgument(2));
            return Mono.<Void>never().doOnCancel(() -> cancelled.set(true));
        });
        when(backend.close(any())).thenReturn(Mono.empty());
        var error = new AtomicReference<Throwable>();
        handler.connect().subscribe(ignored -> { }, error::set);
        handler.abort();
        assertThat(cancelled).isTrue();
        assertThat(error.get()).hasMessageContaining("handshake failed");

        // A blocking container can finish onOpen even after its reactive subscription was cancelled.
        StepVerifier.create(endpoint.get().handle(backend)).verifyComplete();
        verify(backend).close(CloseStatus.GOING_AWAY);
        verify(backend, never()).receive();
    }
}
