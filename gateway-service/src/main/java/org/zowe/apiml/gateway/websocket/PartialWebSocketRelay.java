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

import jakarta.websocket.*;
import org.apache.tomcat.websocket.WsSession;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLContext;

/** One connection pair. Jakarta callbacks only admit owned parts; bounded workers perform all data sends. */
public final class PartialWebSocketRelay {
    private final WebSocketProperties properties;
    private final WebSocketRelayResources resources;
    private final WebSocketHandshakePolicy policy;
    private final WsWebSocketContainer container;
    private final SSLContext sslContext;
    private final URI destination;
    private final HttpHeaders headers;
    private final List<String> protocols;
    private final String serviceId;
    private final long binaryLimit;
    private final long textLimit;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean started = new AtomicBoolean();
    private final Sinks.One<String> negotiated = Sinks.one();
    private final Sinks.Empty<Void> completion = Sinks.empty();
    private final Direction toBackend = new Direction();
    private final Direction toClient = new Direction();
    private final long created = System.nanoTime();
    private volatile Future<?> connecting;
    private volatile Session client;
    private volatile Session backend;

    public PartialWebSocketRelay(WebSocketProperties properties, WebSocketRelayResources resources,
        WebSocketHandshakePolicy policy, WsWebSocketContainer container, SSLContext sslContext,
        URI destination, HttpHeaders headers, List<String> protocols, String serviceId, int legacyLimit) {
        this.properties = properties;
        this.resources = resources;
        this.policy = policy;
        this.container = container;
        this.sslContext = sslContext;
        this.destination = destination;
        this.headers = headers;
        this.protocols = List.copyOf(protocols);
        this.serviceId = serviceId;
        binaryLimit = properties.getPartial().getMaxBinaryMessageSize() == 0 ? legacyLimit : properties.getPartial().getMaxBinaryMessageSize();
        textLimit = properties.getPartial().getMaxTextMessageSize() == 0 ? legacyLimit : properties.getPartial().getMaxTextMessageSize();
        if (!resources.register(this)) {
            throw new RejectedExecutionException("WebSocket connection capacity exhausted");
        }
    }

    public Endpoint clientEndpoint() {
        return new RelayEndpoint(true);
    }

    public Mono<String> connectBackend() {
        return Mono.defer(() -> {
            if (started.compareAndSet(false, true) && !closed.get()) {
                try {
                    connecting = resources.connectors.submit(this::connect);
                    if (closed.get()) connecting.cancel(true);
                } catch (RejectedExecutionException e) {
                    fail("capacity", "WebSocket connection capacity exhausted");
                }
            }
            return negotiated.asMono();
        });
    }

    public Mono<Void> completion() {
        return completion.asMono().doOnCancel(() -> fail("cancelled", "WebSocket relay cancelled"));
    }

    private class RelayEndpoint extends Endpoint {
        private final boolean clientSide;

        RelayEndpoint(boolean clientSide) {
            this.clientSide = clientSide;
        }

        @Override
        public void onOpen(Session session, EndpointConfig config) {
            if (closed.get()) {
                closeSession(session, new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Relay closed"));
                return;
            }
            if (clientSide) {
                client = session;
                if (policy.rejects(new WebSocketHandshakePolicy.Expected(serviceId, session.getNegotiatedSubprotocol()), backend.getNegotiatedSubprotocol())) {
                    fail("subprotocol", WebSocketHandshakePolicy.MISMATCH);
                    return;
                }
                toBackend.install(session);
                toBackend.ready(backend);
                toClient.ready(session);
            } else {
                backend = session;
                // Keep backend reads suspended until the client upgrade has installed its endpoint.
                toClient.install(session);
            }
            if (closed.get()) {
                closeSession(session, new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Relay closed"));
            }
        }

        @Override
        public void onClose(Session session, CloseReason reason) {
            terminate(adapt(reason));
        }

        @Override
        public void onError(Session session, Throwable error) {
            fail("transport", "WebSocket transport failed");
        }
    }

    private void connect() {
        if (closed.get()) {
            return;
        }
        var configuration = ClientEndpointConfig.Builder.create().sslContext(sslContext)
            .preferredSubprotocols(protocols).configurator(new ClientEndpointConfig.Configurator() {
                @Override
                public void beforeRequest(Map<String, List<String>> requestHeaders) {
                    headers.forEach((name, values) -> requestHeaders.put(name, List.copyOf(values)));
                }
            }).build();
        configuration.getUserProperties().put(org.apache.tomcat.websocket.Constants.IO_TIMEOUT_MS_PROPERTY,
            Long.toString(properties.getConnectTimeout()));
        try {
            Session session = container.connectToServer(new RelayEndpoint(false), configuration, destination);
            if (closed.get()) {
                closeSession(session, new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "Relay closed"));
            } else {
                negotiated.tryEmitValue(java.util.Objects.requireNonNullElse(session.getNegotiatedSubprotocol(), ""));
            }
        } catch (Exception e) {
            fail("connect", "WebSocket backend connection failed");
        }
    }

    public void fail(String category, String reason) {
        var status = "connect-timeout".equals(category) ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        if (terminate(new CloseReason("size".equals(category) ? CloseReason.CloseCodes.TOO_BIG : CloseReason.CloseCodes.UNEXPECTED_CONDITION, reason), status)) {
            resources.failure(category);
        }
    }

    private boolean terminate(CloseReason reason) {
        return terminate(reason, HttpStatus.BAD_GATEWAY);
    }

    private boolean terminate(CloseReason reason, HttpStatus status) {
        if (!closed.compareAndSet(false, true)) {
            return false;
        }
        negotiated.tryEmitError(new ResponseStatusException(status, reason.getReasonPhrase()));
        var task = connecting;
        if (task != null && backend == null) {
            task.cancel(true);
        }
        resources.finish(this, () -> {
            toBackend.discard();
            toClient.discard();
            closeSession(client, reason);
            closeSession(backend, reason);
            completion.tryEmitEmpty();
        });
        return true;
    }

    private static CloseReason adapt(CloseReason reason) {
        int code = reason.getCloseCode().getCode();
        if (code == 1005 || code == 1006 || code == 1015) {
            return new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "WebSocket peer disconnected");
        }
        return reason;
    }

    private static void closeSession(Session session, CloseReason reason) {
        if (session != null && session.isOpen()) {
            try {
                session.close(reason);
            } catch (IOException | RuntimeException ignored) {
                // The peer may already have disconnected.
            }
        }
    }

    void checkTimeouts() {
        long now = System.nanoTime();
        if (backend == null && now - created > TimeUnit.MILLISECONDS.toNanos(properties.getConnectTimeout())) {
            fail("connect-timeout", "WebSocket backend connection timed out");
        }
        toBackend.checkTimeout(now);
        toClient.checkTimeout(now);
    }

    private record Part(Object payload, boolean last, long bytes, long received) { }

    private final class Direction {
        private final ArrayDeque<Part> queue = new ArrayDeque<>();
        private Session source;
        private Session target;
        private boolean paused;
        private boolean scheduled;
        private Part inFlight;
        private long pending;
        private long messageSize;
        private long messageStarted;

        synchronized void install(Session session) {
            if (closed.get()) {
                return;
            }
            properties.configure(session);
            session.setMaxBinaryMessageBufferSize(properties.getPartial().getBinaryBufferSize());
            session.setMaxTextMessageBufferSize(properties.getPartial().getTextBufferSize());
            source = session;
            pause();
            session.addMessageHandler(ByteBuffer.class, (MessageHandler.Partial<ByteBuffer>) (data, last) -> receive(data, last));
            session.addMessageHandler(String.class, (MessageHandler.Partial<String>) (data, last) -> receive(data, last));
        }

        synchronized void ready(Session destination) {
            target = destination;
            if (!closed.get()) {
                if (queue.isEmpty()) {
                    resume();
                } else if (!scheduled) {
                    schedule();
                }
            }
        }

        private void receive(Object payload, boolean last) {
            String failure = null;
            synchronized (this) {
                if (closed.get()) {
                    return;
                }
                boolean binary = payload instanceof ByteBuffer;
                long size = binary ? ((ByteBuffer) payload).remaining() : ((String) payload).length();
                long bytes = (binary ? size : 5 * size) + 64;
                long limit = binary ? binaryLimit : textLimit;
                if (size > limit - messageSize) {
                    failure = "size";
                } else if (queue.size() + (inFlight == null ? 0 : 1) >= properties.getPartial().getPendingParts()
                    || bytes > properties.getPartial().getPendingBytes() - pending || !resources.reserve(bytes)) {
                    failure = "capacity";
                } else {
                    long now = System.nanoTime();
                    if (messageStarted == 0) {
                        messageStarted = now;
                    }
                    messageSize += size;
                    if (last) {
                        messageSize = 0;
                        messageStarted = 0;
                    }
                    Object owned = payload;
                    if (binary) {
                        var copy = ByteBuffer.allocate((int) size);
                        copy.put(((ByteBuffer) payload).duplicate()).flip();
                        owned = copy;
                    }
                    queue.add(new Part(owned, last, bytes, now));
                    pending += bytes;
                    pause();
                    if (!scheduled && target != null) {
                        schedule();
                    }
                }
            }
            if (failure != null) {
                fail(failure, "size".equals(failure) ? "WebSocket message limit exceeded" : "WebSocket pending capacity exhausted");
            }
        }

        private void schedule() {
            scheduled = true;
            try {
                resources.writers.execute(this::write);
            } catch (RejectedExecutionException e) {
                scheduled = false;
                fail("capacity", "WebSocket writer capacity exhausted");
            }
        }

        private void write() {
            Part part;
            synchronized (this) {
                if (closed.get() || queue.isEmpty()) {
                    scheduled = false;
                    return;
                }
                part = queue.removeFirst();
                inFlight = part;
            }
            long started = System.nanoTime();
            try {
                if (part.payload() instanceof ByteBuffer data) {
                    target.getBasicRemote().sendBinary(data, part.last());
                } else {
                    target.getBasicRemote().sendText((String) part.payload(), part.last());
                }
            } catch (IOException | RuntimeException e) {
                fail("send", "WebSocket send failed");
            } finally {
                resources.writes.record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
                synchronized (this) {
                    inFlight = null;
                    pending -= part.bytes();
                    resources.release(part.bytes());
                    scheduled = false;
                    if (!closed.get()) {
                        if (queue.isEmpty()) {
                            resume();
                        } else {
                            schedule();
                        }
                    }
                }
            }
        }

        private void pause() {
            if (!paused) {
                ((WsSession) source).suspend();
                paused = true;
                resources.paused.incrementAndGet();
            }
        }

        private void resume() {
            if (paused) {
                paused = false;
                resources.paused.decrementAndGet();
                ((WsSession) source).resume();
            }
        }

        synchronized void discard() {
            Part part;
            while ((part = queue.pollFirst()) != null) {
                pending -= part.bytes();
                resources.release(part.bytes());
            }
            if (paused) {
                paused = false;
                resources.paused.decrementAndGet();
            }
        }

        void checkTimeout(long now) {
            boolean sendTimedOut;
            boolean messageTimedOut;
            synchronized (this) {
                Part oldest = inFlight == null ? queue.peekFirst() : inFlight;
                sendTimedOut = oldest != null && now - oldest.received() > TimeUnit.MILLISECONDS.toNanos(properties.getAsyncWriteTimeout());
                messageTimedOut = messageStarted != 0 && now - messageStarted > TimeUnit.MILLISECONDS.toNanos(properties.getPartial().getMessageTimeout());
            }
            if (sendTimedOut) {
                fail("send-timeout", "WebSocket send timed out");
            } else if (messageTimedOut) {
                fail("message-timeout", "WebSocket unfinished message timed out");
            }
        }
    }
}
