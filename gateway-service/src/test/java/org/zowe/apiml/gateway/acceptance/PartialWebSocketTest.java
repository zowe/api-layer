/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import jakarta.websocket.*;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.apache.tomcat.websocket.WsSession;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockWebSocketStreamingService;
import org.zowe.apiml.gateway.MockWebSocketStreamingService.Received;
import org.zowe.apiml.gateway.RawWebSocketClient;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@MicroservicesAcceptanceTest
@TestPropertySource(properties = {
    "server.webSocket.partial.maxBinaryMessageSize=16777216",
    "server.webSocket.partial.maxTextMessageSize=16777216",
    "spring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=3145728"
})
class PartialWebSocketTest extends AcceptanceTestWithMockServices {
    @Autowired
    MeterRegistry meters;
    @TempDir
    Path directory;
    private MockWebSocketStreamingService backend;
    private WsWebSocketContainer container;
    private final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    private final BlockingQueue<CloseReason> closed = new LinkedBlockingQueue<>();
    private Session client;

    private void start(Boolean partial, List<String> protocols) throws Exception {
        start(partial, protocols, null);
    }

    private void start(Boolean partial, List<String> protocols, Path keyStore) throws Exception {
        backend = new MockWebSocketStreamingService("streamingservice", directory, partial, protocols, service -> {
            applicationRegistry.update(service);
            updateRoutingRules();
        });
        if (keyStore != null) backend.useTls(keyStore, Path.of("../keystore/service/service.truststore.p12"), "password");
        backend.start();
    }

    private void connect(List<String> protocols, boolean compression) throws Exception {
        if (container == null) container = new WsWebSocketContainer();
        var builder = ClientEndpointConfig.Builder.create().sslContext(apimlSSLContext).preferredSubprotocols(protocols);
        if (compression) {
            builder.extensions(container.getInstalledExtensions().stream()
                .filter(extension -> "permessage-deflate".equals(extension.getName())).toList());
        }
        client = container.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig config) {
                session.setMaxBinaryMessageBufferSize(65_536);
                session.setMaxTextMessageBufferSize(32_768);
                session.addMessageHandler(ByteBuffer.class, (MessageHandler.Partial<ByteBuffer>) (data, last) -> {
                    byte[] bytes = new byte[data.remaining()];
                    data.get(bytes);
                    received.add(new Received(bytes, last));
                });
                session.addMessageHandler(String.class, (MessageHandler.Partial<String>) (data, last) -> received.add(new Received(data, last)));
            }

            @Override
            public void onClose(Session session, CloseReason reason) {
                closed.add(reason);
            }
        }, builder.build(), URI.create("wss://localhost:" + port + "/streamingservice/ws/v1"));
    }

    private static Received next(BlockingQueue<Received> queue) throws Exception {
        var part = queue.poll(10, TimeUnit.SECONDS);
        assertThat(part).as("part must arrive while FIN is withheld").isNotNull();
        return part;
    }

    @AfterEach
    void closeClient() throws Exception {
        if (client != null && client.isOpen()) {
            client.close();
        }
        if (container != null) container.destroy();
        client = null;
        container = null;
        received.clear();
        closed.clear();
    }

    @Test
    void defaultsToForwardingTextBeforeFinInBothDirectionsAndPreservesEmptyFinalParts() throws Exception {
        start(null, List.of());
        connect(List.of(), true);
        assertThat(client.getNegotiatedExtensions()).isEmpty();
        var service = backend.opened.poll(10, TimeUnit.SECONDS);
        assertThat(service).isNotNull();
        client.getBasicRemote().sendText("hello €", false);
        assertThat(next(backend.received)).isEqualTo(new Received("hello €", false));
        service.getBasicRemote().sendText("reply 🚀", false);
        assertThat(next(received)).isEqualTo(new Received("reply 🚀", false));
        client.getBasicRemote().sendText("", true);
        assertThat(next(backend.received)).isEqualTo(new Received("", true));
        service.getBasicRemote().sendText("", true);
        assertThat(next(received)).isEqualTo(new Received("", true));
        assertThat(backend.connections).hasValue(1);
    }

    @Test
    void forwardsCompressibleBinaryBeforeFinInBothDirections() throws Exception {
        start(true, List.of());
        connect(List.of(), true);
        var service = backend.opened.poll(10, TimeUnit.SECONDS);
        byte[] data = new byte[65_536];
        client.getBasicRemote().sendBinary(ByteBuffer.wrap(data), false);
        var inbound = next(backend.received);
        assertThat((byte[]) inbound.data()).isEqualTo(data);
        assertThat(inbound.last()).isFalse();
        service.getBasicRemote().sendBinary(ByteBuffer.wrap(data), false);
        var outbound = next(received);
        assertThat((byte[]) outbound.data()).isEqualTo(data);
        assertThat(outbound.last()).isFalse();
        client.getBasicRemote().sendBinary(ByteBuffer.allocate(0), true);
        assertThat(next(backend.received).last()).isTrue();
        service.getBasicRemote().sendBinary(ByteBuffer.allocate(0), true);
        assertThat(next(received).last()).isTrue();
    }

    @Test
    void forwardsSixteenMiBAsOneMessageWithoutAggregating() throws Exception {
        start(true, List.of());
        connect(List.of(), false);
        byte[] chunk = new byte[65_536];
        for (int i = 0; i < chunk.length; i++) chunk[i] = (byte) i;
        long size = 0;
        int completed = 0;
        for (int part = 0; part < 256; part++) {
            client.getBasicRemote().sendBinary(ByteBuffer.wrap(chunk), part == 255);
            var next = next(backend.received);
            assertThat((byte[]) next.data()).isEqualTo(chunk);
            size += ((byte[]) next.data()).length;
            if (next.last()) completed++;
        }
        assertThat(size).isEqualTo(16 * 1024 * 1024);
        assertThat(completed).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void negotiatesTheBackendProtocolWhenOnlyALaterClientOfferIsSupported(boolean partial) throws Exception {
        start(partial, List.of("stomp3", "stomp4"));
        connect(List.of("stomp1", "stomp2", "stomp3"), false);
        var service = backend.opened.poll(10, TimeUnit.SECONDS);
        assertThat(service).isNotNull();
        assertThat(client.getNegotiatedSubprotocol()).isEqualTo("stomp3");
        assertThat(service.getNegotiatedSubprotocol()).isEqualTo("stomp3");
        client.getBasicRemote().sendText("request", true);
        assertThat(next(backend.received)).isEqualTo(new Received("request", true));
        service.getBasicRemote().sendText("response", true);
        assertThat(next(received)).isEqualTo(new Received("response", true));
        assertThat(backend.connections).hasValue(1);
        assertThat(closed).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void preservesTheBackendSelectingNoProtocol(boolean partial) throws Exception {
        start(partial, List.of());
        connect(List.of("first"), false);
        assertThat(client.getNegotiatedSubprotocol()).isEmpty();
        assertThat(backend.opened.poll(10, TimeUnit.SECONDS).getNegotiatedSubprotocol()).isEmpty();
        client.getBasicRemote().sendText("no subprotocol", true);
        assertThat(next(backend.received)).isEqualTo(new Received("no subprotocol", true));
        assertThat(backend.connections).hasValue(1);
        assertThat(closed).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void retainsTheBackendGreetingUntilTheClientUpgradeCompletes(boolean partial) throws Exception {
        start(partial, List.of("second"));
        backend.onOpen = session -> {
            try {
                session.getBasicRemote().sendText("greeting", true);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        };
        connect(List.of("first", "second"), false);
        assertThat(client.getNegotiatedSubprotocol()).isEqualTo("second");
        assertThat(next(received)).isEqualTo(new Received("greeting", true));
        assertThat(backend.connections).hasValue(1);
    }

    @Test
    void aggregationRetainsCompressionAndWholeMessageForwarding() throws Exception {
        start(false, List.of());
        connect(List.of(), true);
        assertThat(client.getNegotiatedExtensions()).extracting(Extension::getName).contains("permessage-deflate");
        client.getBasicRemote().sendText("first", false);
        client.getBasicRemote().sendText("last", true);
        assertThat(next(backend.received)).isEqualTo(new Received("firstlast", true));
    }

    @Test
    void forwardsWithinASingleLargeFrame() throws Exception {
        start(true, List.of());
        try (var raw = new RawWebSocketClient(apimlSSLContext, port, "/streamingservice/ws/v1")) {
            byte[] chunk = new byte[65_536];
            raw.header(2, true, 256 * chunk.length);
            for (int i = 0; i < 256; i++) {
                // Tomcat detects a full staging buffer when the next payload byte arrives.
                raw.payload(java.util.Arrays.copyOf(chunk, chunk.length + (i == 0 ? 1 : i == 255 ? -1 : 0)));
                // The sender has not written the rest of this single frame yet.
                var part = next(backend.received);
                assertThat((byte[]) part.data()).isEqualTo(chunk);
                assertThat(part.last()).isEqualTo(i == 255);
            }
        }
    }

    @Test
    void decodesUtf8SplitAcrossFramesAndKeepsMixedMessageBoundaries() throws Exception {
        start(true, List.of());
        try (var raw = new RawWebSocketClient(apimlSSLContext, port, "/streamingservice/ws/v1")) {
            raw.frame(1, false, new byte[] {'a', (byte) 0xf0, (byte) 0x9f});
            raw.frame(0, true, new byte[] {(byte) 0x9a, (byte) 0x80, 'z'});
            StringBuilder message = new StringBuilder();
            Received part;
            do {
                part = next(backend.received);
                message.append((String) part.data());
            } while (!part.last());
            assertThat(message.toString()).isEqualTo("a🚀z");
            raw.frame(2, true, new byte[] {1, 2, 3});
            assertThat((byte[]) next(backend.received).data()).containsExactly(1, 2, 3);
            raw.frame(1, true, new byte[0]);
            assertThat(next(backend.received)).isEqualTo(new Received("", true));
        }
    }

    @Test
    void invalidTextClosesThePairWithoutCompletingAMessage() throws Exception {
        start(true, List.of());
        try (var raw = new RawWebSocketClient(apimlSSLContext, port, "/streamingservice/ws/v1")) {
            assertThat(backend.opened.poll(10, TimeUnit.SECONDS)).isNotNull();
            raw.frame(1, true, new byte[] {(byte) 0xc3, 0x28});
            assertThat(backend.closed.poll(10, TimeUnit.SECONDS)).isNotNull();
            assertThat(backend.received).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptsMatchingProtocolsWithOneBackendConnection(boolean partial) throws Exception {
        start(partial, List.of("first", "second"));
        connect(List.of("first", "second"), false);
        var service = backend.opened.poll(10, TimeUnit.SECONDS);
        assertThat(service).isNotNull();
        assertThat(client.getNegotiatedSubprotocol()).isEqualTo("first");
        assertThat(service.getNegotiatedSubprotocol()).isEqualTo("first");
        client.getBasicRemote().sendText("message", true);
        assertThat(next(backend.received)).isEqualTo(new Received("message", true));
        assertThat(backend.connections).hasValue(1);
    }

    @Test
    void usesConfiguredTlsAndClientCertificateForPartialBackendConnections() throws Exception {
        start(true, List.of(), Path.of("../keystore/service/service.keystore.p12"));
        connect(List.of(), false);
        client.getBasicRemote().sendText("mutual TLS", false);
        assertThat(next(backend.received)).isEqualTo(new Received("mutual TLS", false));
    }

    @Test
    void preservesStrictHostnameVerificationForPartialBackendConnections() throws Exception {
        start(true, List.of(), Path.of("../keystore/negative/hostname-mismatch.keystore.p12"));
        assertThatThrownBy(() -> connect(List.of(), false)).isInstanceOf(DeploymentException.class)
            .hasMessageContaining("502");
        assertThat(backend.connections).hasValue(0);
    }

    @Test
    void answersPingsOnEachLegDuringAnUnfinishedMessage() throws Exception {
        start(true, List.of());
        connect(List.of(), false);
        var service = backend.opened.poll(10, TimeUnit.SECONDS);
        assertThat(service).isNotNull();
        var pongs = new LinkedBlockingQueue<ByteBuffer>();
        client.addMessageHandler(PongMessage.class, (MessageHandler.Whole<PongMessage>) pong -> pongs.add(pong.getApplicationData()));
        service.addMessageHandler(PongMessage.class, (MessageHandler.Whole<PongMessage>) pong -> pongs.add(pong.getApplicationData()));
        client.getBasicRemote().sendText("incomplete", false);
        next(backend.received);
        client.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[] {1}));
        assertThat(pongs.poll(10, TimeUnit.SECONDS)).isEqualTo(ByteBuffer.wrap(new byte[] {1}));
        service.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[] {2}));
        assertThat(pongs.poll(10, TimeUnit.SECONDS)).isEqualTo(ByteBuffer.wrap(new byte[] {2}));
        service.close(new CloseReason(CloseReason.CloseCodes.GOING_AWAY, "test close"));
        var reason = closed.poll(10, TimeUnit.SECONDS);
        assertThat(reason).isNotNull();
        assertThat(reason.getCloseCode().getCode()).isEqualTo(1001);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void slowReceiverBoundsPendingDataAndSlowsTheSource(boolean towardsBackend) throws Exception {
        start(true, List.of());
        connect(List.of(), false);
        var service = backend.opened.poll(10, TimeUnit.SECONDS);
        assertThat(service).isNotNull();
        var source = towardsBackend ? client : service;
        var target = (WsSession) (towardsBackend ? service : client);
        var parts = towardsBackend ? backend.received : received;
        var count = new AtomicInteger();
        var executor = Executors.newSingleThreadExecutor();
        target.suspend();
        var sending = executor.submit(() -> {
            byte[] bytes = new byte[65_536];
            for (int i = 0; i < 1024; i++) {
                source.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes), true);
                count.incrementAndGet();
            }
            return null;
        });
        try {
            await().atMost(Duration.ofSeconds(10)).during(Duration.ofMillis(300)).untilAsserted(() -> {
                assertThat(meters.get("apiml.websocket.pending.bytes").gauge().value()).isBetween(1d, 1_048_576d);
                assertThat(meters.get("apiml.websocket.paused.reads").gauge().value()).isPositive();
                assertThat(count.get()).isLessThan(1024);
                assertThat(sending.isDone()).isFalse();
            });
            // The bounded blocking writers must not occupy the shared HTTP connector threads.
            io.restassured.RestAssured.given().config(apimlCert)
                .get("https://localhost:" + port + "/application/info").then().statusCode(200);
            target.resume();
            sending.get(30, TimeUnit.SECONDS);
            long bytes = 0;
            int completed = 0;
            while (completed < 1024) {
                var part = next(parts);
                bytes += ((byte[]) part.data()).length;
                if (part.last()) completed++;
            }
            assertThat(bytes).isEqualTo(64L * 1024 * 1024);
            await().untilAsserted(() -> assertThat(meters.get("apiml.websocket.pending.bytes").gauge().value()).isZero());
        } finally {
            target.resume();
            sending.cancel(true);
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @CsvSource({"false,1,1", "true,1,1", "false,32,16", "true,32,16"})
    void concurrentBidirectionalTransfersKeepTheirContentAndBoundaries(boolean partial, int connections, int chunks) throws Exception {
        start(partial, List.of());
        var clients = new java.util.ArrayList<Session>();
        var services = new java.util.ArrayList<Session>();
        var clientParts = new java.util.ArrayList<BlockingQueue<Received>>();
        var serviceParts = new java.util.ArrayList<BlockingQueue<Received>>();
        var executor = Executors.newFixedThreadPool(8);
        try {
            for (int i = 0; i < connections; i++) {
                connect(List.of(), false);
                var service = backend.opened.poll(10, TimeUnit.SECONDS);
                assertThat(service).isNotNull();
                clients.add(client);
                services.add(service);
                clientParts.add(captureBinary(client));
                serviceParts.add(captureBinary(service));
            }
            long started = System.nanoTime();
            long gcBefore = gcCollections();
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < connections; i++) {
                final int connection = i;
                tasks.add(executor.submit(() -> {
                    byte[] bytes = new byte[65_536];
                    java.util.Arrays.fill(bytes, (byte) connection);
                    for (int part = 0; part < chunks; part++) {
                        clients.get(connection).getBasicRemote().sendBinary(ByteBuffer.wrap(bytes), part == chunks - 1);
                        services.get(connection).getBasicRemote().sendBinary(ByteBuffer.wrap(bytes), part == chunks - 1);
                    }
                    return null;
                }));
            }
            for (var task : tasks) task.get(30, TimeUnit.SECONDS);
            for (int i = 0; i < connections; i++) {
                for (var queue : List.of(clientParts.get(i), serviceParts.get(i))) {
                    long size = 0;
                    Received part;
                    do {
                        part = next(queue);
                        byte[] bytes = (byte[]) part.data();
                        byte[] expected = new byte[bytes.length];
                        java.util.Arrays.fill(expected, (byte) i);
                        assertThat(bytes).isEqualTo(expected);
                        size += bytes.length;
                    } while (!part.last());
                    assertThat(size).isEqualTo((long) chunks * 65_536);
                    assertThat(queue).isEmpty();
                }
            }
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            long writerCount = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith("websocket-writer-")).count();
            assertThat(writerCount).isLessThanOrEqualTo(16);
            System.out.printf("WebSocket transfer sample: partial=%s, connections=%d, bytesEachDirection=%d, ms=%d, writerThreads=%d, gcCollections=%d%n",
                partial, connections, (long) chunks * 65_536, millis, writerCount, gcCollections() - gcBefore);
        } finally {
            for (var session : clients) if (session.isOpen()) session.close();
            executor.shutdownNow();
        }
    }

    private static BlockingQueue<Received> captureBinary(Session session) {
        new java.util.ArrayList<>(session.getMessageHandlers()).forEach(session::removeMessageHandler);
        var queue = new LinkedBlockingQueue<Received>();
        session.addMessageHandler(ByteBuffer.class, (MessageHandler.Partial<ByteBuffer>) (data, last) -> {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            queue.add(new Received(bytes, last));
        });
        return queue;
    }

    private static long gcCollections() {
        return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum();
    }
}
