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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Retains whole-message forwarding while allowing the backend to select the shared subprotocol. */
public final class AggregatingWebSocketHandler implements BackendWebSocketHandler {
    private final WebSocketClient client;
    private final URI destination;
    private final HttpHeaders headers;
    private final List<String> protocols;
    private final String serviceId;
    private final WebSocketHandshakePolicy policy;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Disposable.Swap connection = Disposables.swap();
    private final Sinks.One<String> negotiated = Sinks.one();
    private final Sinks.One<WebSocketSession> frontend = Sinks.one();
    private final Sinks.Empty<Void> completion = Sinks.empty();
    private volatile WebSocketSession backend;

    public AggregatingWebSocketHandler(WebSocketClient client, URI destination, HttpHeaders headers,
        List<String> protocols, String serviceId, WebSocketHandshakePolicy policy) {
        this.client = client;
        this.destination = destination;
        this.headers = headers;
        this.protocols = List.copyOf(protocols);
        this.serviceId = serviceId;
        this.policy = policy;
    }

    @Override
    public List<String> getSubProtocols() {
        return protocols;
    }

    @Override
    public Mono<String> connect() {
        return Mono.deferContextual(context -> {
            if (started.compareAndSet(false, true) && !closed.get()) {
                connection.update(client.execute(destination, headers, new WebSocketHandler() {
                    @Override
                    public List<String> getSubProtocols() {
                        return protocols;
                    }

                    @Override
                    public Mono<Void> handle(WebSocketSession session) {
                        backend = session;
                        if (closed.get()) return session.close(CloseStatus.GOING_AWAY);
                        // TomcatWebSocketSession starts with reads suspended, until forwarding subscribes.
                        negotiated.tryEmitValue(Objects.requireNonNullElse(session.getHandshakeInfo().getSubProtocol(), ""));
                        return frontend.asMono().flatMap(front -> forward(front, session));
                    }
                }).contextWrite(context).subscribe(ignored -> { }, error -> {
                    negotiated.tryEmitError(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WebSocket backend connection failed", error));
                    completion.tryEmitError(error);
                    abort();
                }, () -> {
                    negotiated.tryEmitError(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WebSocket backend connection closed"));
                    completion.tryEmitEmpty();
                    closed.set(true);
                }));
            }
            return negotiated.asMono();
        });
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        if (closed.get()) return session.close(CloseStatus.SERVER_ERROR);
        frontend.tryEmitValue(session);
        return completion.asMono();
    }

    private Mono<Void> forward(WebSocketSession front, WebSocketSession back) {
        if (policy.rejects(new WebSocketHandshakePolicy.Expected(serviceId, front.getHandshakeInfo().getSubProtocol()),
            back.getHandshakeInfo().getSubProtocol())) {
            return Mono.error(new IllegalStateException(WebSocketHandshakePolicy.MISMATCH));
        }
        return Mono.when(
            back.send(front.receive().map(WebSocketMessage::retain)),
            front.send(back.receive().map(WebSocketMessage::retain)),
            front.closeStatus().flatMap(status -> back.isOpen() ? back.close(adapt(status)) : Mono.empty()),
            back.closeStatus().flatMap(status -> front.isOpen() ? front.close(adapt(status)) : Mono.empty()));
    }

    private static CloseStatus adapt(CloseStatus status) {
        return switch (status.getCode()) {
            case 1004, 1005, 1006, 1015 -> CloseStatus.PROTOCOL_ERROR;
            default -> status;
        };
    }

    @Override
    public void abort() {
        if (closed.compareAndSet(false, true)) {
            negotiated.tryEmitError(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "WebSocket handshake failed"));
            completion.tryEmitError(new IllegalStateException("WebSocket handshake failed"));
            connection.dispose();
            frontend.tryEmitError(new IllegalStateException("WebSocket handshake failed"));
            var session = backend;
            if (session != null) session.close(CloseStatus.SERVER_ERROR).subscribe(ignored -> { }, ignored -> { });
        }
    }
}
