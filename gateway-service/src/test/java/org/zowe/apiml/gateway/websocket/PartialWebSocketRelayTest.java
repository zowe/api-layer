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
import jakarta.websocket.*;
import org.apache.tomcat.websocket.WsSession;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PartialWebSocketRelayTest {
    private WebSocketProperties properties;
    private WebSocketRelayResources resources;
    private WsWebSocketContainer container;
    private Peer client;
    private Peer backend;
    private PartialWebSocketRelay relay;

    @BeforeEach
    void setup() throws Exception {
        properties = new WebSocketProperties();
        properties.getPartial().setWriterThreads(1);
        properties.getPartial().setMaxConnections(2);
        container = mock(WsWebSocketContainer.class);
        client = new Peer();
        backend = new Peer();
        when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class))).thenAnswer(call -> {
            call.<Endpoint>getArgument(0).onOpen(backend.session, call.getArgument(1));
            return backend.session;
        });
    }

    private void start() {
        resources = new WebSocketRelayResources(properties, new SimpleMeterRegistry());
        relay = create();
        relay.connectBackend().block(Duration.ofSeconds(3));
        relay.clientEndpoint().onOpen(client.session, mock(EndpointConfig.class));
        await().untilAsserted(() -> verify(backend.session).resume());
    }

    private PartialWebSocketRelay create() {
        return new PartialWebSocketRelay(properties, resources, new WebSocketHandshakePolicy(), container, null,
            URI.create("ws://localhost/service"), new HttpHeaders(), List.of(), "service", 8192);
    }

    @AfterEach
    void shutdown() {
        if (resources != null) resources.close();
    }

    @Nested
    class FlowControl {
        @Test
        void enforcesTheGlobalBudgetAcrossConnectionsAndBothDirections() throws Exception {
            properties.getPartial().setGlobalPendingBytes(2_097_152);
            properties.getPartial().setMaxBinaryMessageSize(16_777_216);
            var secondClient = new Peer();
            var secondBackend = new Peer();
            var connected = new java.util.concurrent.atomic.AtomicInteger();
            when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class))).thenAnswer(call -> {
                var session = connected.getAndIncrement() == 0 ? backend.session : secondBackend.session;
                call.<Endpoint>getArgument(0).onOpen(session, call.getArgument(1));
                return session;
            });
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            doAnswer(call -> {
                entered.countDown();
                release.await(5, TimeUnit.SECONDS);
                return null;
            }).when(backend.basic).sendBinary(any(), anyBoolean());
            start();
            var second = create();
            second.connectBackend().block(Duration.ofSeconds(3));
            second.clientEndpoint().onOpen(secondClient.session, mock(EndpointConfig.class));
            await().untilAsserted(() -> verify(secondBackend.session).resume());
            try {
                client.binary(ByteBuffer.allocate(65_536), false);
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                for (int i = 1; i < 8; i++) client.binary(ByteBuffer.allocate(65_536), false);
                for (var peer : List.of(backend, secondClient, secondBackend)) {
                    for (int i = 0; i < 8; i++) peer.binary(ByteBuffer.allocate(65_536), false);
                }
                await().untilAsserted(() -> assertThat(secondClient.close.get()).isNotNull());
                assertThat(secondClient.close.get().getReasonPhrase()).contains("capacity exhausted");
                assertThat(client.close.get()).isNull();
                assertThat(resources.pendingBytes()).isBetween(1L, 2_097_152L);
                assertThat(resources.writers.getLargestPoolSize()).isEqualTo(1);
            } finally {
                release.countDown();
            }
            await().untilAsserted(() -> assertThat(resources.pendingBytes()).isZero());
        }

        @Test
        void ownsDeferredBinaryDataAndKeepsWritesOrdered() throws Exception {
            var blocked = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            doAnswer(call -> {
                blocked.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                return null;
            }).doNothing().when(backend.basic).sendBinary(any(), anyBoolean());
            start();
            try {
                client.binary(ByteBuffer.wrap(new byte[]{1}), false);
                assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
                byte[] original = {2, 3};
                client.binary(ByteBuffer.wrap(original), true);
                original[0] = 99;
                assertThat(resources.pendingBytes()).isPositive();
                verify(client.session, atLeast(2)).suspend();
                release.countDown();
                await().untilAsserted(() -> {
                    var order = inOrder(backend.basic);
                    order.verify(backend.basic).sendBinary(ByteBuffer.wrap(new byte[]{1}), false);
                    order.verify(backend.basic).sendBinary(ByteBuffer.wrap(new byte[]{2, 3}), true);
                    assertThat(resources.pendingBytes()).isZero();
                });
            } finally {
                release.countDown();
            }
        }

        @Test
        void queueExhaustionClosesBothLegsAndReleasesOwnedParts() throws Exception {
            properties.getPartial().setPendingParts(2);
            var blocked = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            doAnswer(call -> {
                blocked.countDown();
                release.await(5, TimeUnit.SECONDS);
                return null;
            }).when(backend.basic).sendText(anyString(), anyBoolean());
            start();
            try {
                client.text("one", false);
                assertThat(blocked.await(5, TimeUnit.SECONDS)).isTrue();
                client.text("two", false);
                client.text("three", true);
                await().untilAsserted(() -> {
                    assertThat(client.close.get().getCloseCode().getCode()).isEqualTo(1011);
                    assertThat(backend.close.get()).isNotNull();
                });
            } finally {
                release.countDown();
            }
            await().untilAsserted(() -> assertThat(resources.pendingBytes()).isZero());
        }

        @Test
        void emptyMessagesConsumeCapacityAndAreForwarded() throws Exception {
            start();
            client.text("", false);
            client.text("", true);
            backend.binary(ByteBuffer.allocate(0), true);
            await().untilAsserted(() -> {
                verify(backend.basic).sendText("", false);
                verify(backend.basic).sendText("", true);
                verify(client.basic).sendBinary(ByteBuffer.allocate(0), true);
                assertThat(resources.pendingBytes()).isZero();
            });
        }
    }

    @Nested
    class LimitsAndLifecycle {
        @Test
        void writeTimeoutIncludesAnInProgressSendAndRetainsItsReservationUntilItReturns() throws Exception {
            properties.setAsyncWriteTimeout(100);
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            doAnswer(call -> {
                entered.countDown();
                release.await(5, TimeUnit.SECONDS);
                return null;
            }).when(backend.basic).sendText(anyString(), anyBoolean());
            start();
            try {
                client.text("blocked", true);
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                await().untilAsserted(() -> assertThat(client.close.get()).isNotNull());
                assertThat(client.close.get().getReasonPhrase()).contains("send timed out");
                assertThat(resources.pendingBytes()).isPositive();
            } finally {
                release.countDown();
            }
            await().untilAsserted(() -> assertThat(resources.pendingBytes()).isZero());
        }

        @Test
        void timesOutAndCancelsABackendHandshake() throws Exception {
            properties.setConnectTimeout(100);
            var interrupted = new CountDownLatch(1);
            when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class))).thenAnswer(call -> {
                try {
                    new CountDownLatch(1).await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    throw e;
                }
                return null;
            });
            resources = new WebSocketRelayResources(properties, new SimpleMeterRegistry());
            relay = create();
            var error = new AtomicReference<Throwable>();
            relay.connectBackend().subscribe(ignored -> { }, error::set);
            assertThat(interrupted.await(3, TimeUnit.SECONDS)).isTrue();
            await().untilAsserted(() -> assertThat(error.get()).hasMessageContaining("connection timed out"));
            assertThat(client.close.get()).isNull();
            assertThat(resources.pendingBytes()).isZero();
        }

        @Test
        void closesAConnectionThatFinishesAfterCancellation() throws Exception {
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class))).thenAnswer(call -> {
                entered.countDown();
                while (release.getCount() > 0) {
                    try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
                }
                call.<Endpoint>getArgument(0).onOpen(backend.session, call.getArgument(1));
                return backend.session;
            });
            resources = new WebSocketRelayResources(properties, new SimpleMeterRegistry());
            relay = create();
            var error = new AtomicReference<Throwable>();
            relay.connectBackend().subscribe(ignored -> { }, error::set);
            try {
                assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
                relay.fail("cancelled", "cancelled during connect");
            } finally {
                release.countDown();
            }
            await().untilAsserted(() -> {
                assertThat(error.get()).hasMessageContaining("cancelled during connect");
                assertThat(backend.close.get()).isNotNull();
            });
            verify(backend.session, never()).addMessageHandler(any(Class.class), any(MessageHandler.Partial.class));
        }

        @Test
        void backendFailureFailsBeforeUpgradingTheClient() throws Exception {
            when(container.connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class)))
                .thenThrow(new java.io.IOException("backend unavailable"));
            resources = new WebSocketRelayResources(properties, new SimpleMeterRegistry());
            relay = create();
            var error = new AtomicReference<Throwable>();
            relay.connectBackend().subscribe(ignored -> { }, error::set);
            await().untilAsserted(() -> assertThat(error.get()).hasMessageContaining("backend connection failed"));
            assertThat(client.close.get()).isNull();
            verify(container).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class));
        }

        @Test
        void inheritsTheLegacyMessageLimitWithoutIncreasingStagingBuffers() {
            start();
            client.binary(ByteBuffer.allocate(8192), false);
            await().untilAsserted(() -> assertThat(resources.pendingBytes()).isZero());
            client.binary(ByteBuffer.allocate(1), true);
            await().untilAsserted(() -> assertThat(client.close.get().getCloseCode().getCode()).isEqualTo(1009));
            verify(client.session).setMaxBinaryMessageBufferSize(65_536);
        }

        @Test
        void closesBothLegsAndDropsQueuedDataOnShutdown() throws Exception {
            start();
            client.text("unfinished", false);
            resources.close();
            await().untilAsserted(() -> {
                assertThat(client.close.get()).isNotNull();
                assertThat(backend.close.get()).isNotNull();
                assertThat(resources.pendingBytes()).isZero();
                assertThat(resources.paused).hasValue(0);
            });
            assertThatThrownBy(PartialWebSocketRelayTest.this::create)
                .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        }

        @Test
        void countsTotalMessageSizeAcrossPartsAndClosesWith1009() throws Exception {
            properties.getPartial().setMaxBinaryMessageSize(4);
            start();
            client.binary(ByteBuffer.wrap(new byte[]{1, 2, 3}), false);
            await().untilAsserted(() -> verify(backend.basic).sendBinary(any(), eq(false)));
            client.binary(ByteBuffer.wrap(new byte[]{4, 5}), true);
            await().untilAsserted(() -> assertThat(client.close.get().getCloseCode().getCode()).isEqualTo(1009));
            verify(backend.basic, never()).sendBinary(any(), eq(true));
        }

        @Test
        void independentMessagesResetTheirSizeCounter() throws Exception {
            properties.getPartial().setMaxTextMessageSize(3);
            start();
            for (int i = 0; i < 10; i++) {
                client.text("abc", true);
                await().untilAsserted(() -> assertThat(resources.pendingBytes()).isZero());
            }
            verify(backend.basic, times(10)).sendText("abc", true);
            assertThat(client.close.get()).isNull();
        }

        @Test
        void unfinishedMessageTimesOutEvenWhenNothingIsQueued() throws Exception {
            properties.getPartial().setMessageTimeout(100);
            start();
            client.text("unfinished", false);
            await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> assertThat(client.close.get()).isNotNull());
            assertThat(client.close.get().getCloseCode().getCode()).isEqualTo(1011);
        }

        @Test
        void rejectsNewConnectionsWhenSlotsAreExhausted() {
            properties.getPartial().setMaxConnections(1);
            start();
            assertThatThrownBy(PartialWebSocketRelayTest.this::create)
                .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        }

        @Test
        void cancellationClosesBothSessions() {
            start();
            var subscription = relay.completion().subscribe();
            subscription.dispose();
            await().untilAsserted(() -> {
                assertThat(client.close.get()).isNotNull();
                assertThat(backend.close.get()).isNotNull();
                assertThat(resources.pendingBytes()).isZero();
            });
        }
    }

    private static class Peer {
        final WsSession session = mock(WsSession.class);
        final RemoteEndpoint.Basic basic = mock(RemoteEndpoint.Basic.class);
        final Map<Class<?>, MessageHandler.Partial<?>> handlers = new java.util.concurrent.ConcurrentHashMap<>();
        final AtomicReference<CloseReason> close = new AtomicReference<>();

        Peer() throws Exception {
            when(session.getBasicRemote()).thenReturn(basic);
            when(session.getAsyncRemote()).thenReturn(mock(RemoteEndpoint.Async.class));
            when(session.getUserProperties()).thenReturn(new HashMap<>());
            when(session.getNegotiatedSubprotocol()).thenReturn("");
            when(session.isOpen()).thenAnswer(call -> close.get() == null);
            doAnswer(call -> {
                handlers.put(call.getArgument(0), call.getArgument(1));
                return null;
            }).when(session).addMessageHandler(any(Class.class), any(MessageHandler.Partial.class));
            doAnswer(call -> { close.set(call.getArgument(0)); return null; }).when(session).close(any(CloseReason.class));
        }

        @SuppressWarnings("unchecked")
        void binary(ByteBuffer data, boolean last) {
            ((MessageHandler.Partial<ByteBuffer>) handlers.get(ByteBuffer.class)).onMessage(data, last);
        }

        @SuppressWarnings("unchecked")
        void text(String data, boolean last) {
            ((MessageHandler.Partial<String>) handlers.get(String.class)).onMessage(data, last);
        }
    }
}
