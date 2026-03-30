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

import groovy.util.logging.Slf4j;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.Opcode;
import org.java_websocket.framing.TextFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * These tests run against different web socket server implementations
 */
class WebSocketTest {

    @MicroservicesAcceptanceTest
    @TestInstance(Lifecycle.PER_CLASS)
    @ActiveProfiles({ "WebSocketTest" })
    @TestPropertySource(
        properties = {
            "spring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=3145728", // bytes,
            "apiml.security.ssl.nonStrictVerifySslCertificatesOfServices=true"
        }
    )
    @Slf4j
    @Nested
    class OnNonStrictHostname extends AcceptanceTestWithMockServices {

        private WebSocketTestClient webSocketClient;

        @BeforeAll
        void setUp() {
            mockServiceWs("websocketservice")
                .sslContext(apimlNonStrictSSLContext)
                .assertion(message -> {
                    if (message instanceof String s) {
                        assertTrue(StringUtils.isNotBlank(s));
                    } else if (message instanceof ByteBuffer b) {
                        assertTrue(b.remaining() > 0);
                    }
                })
                .start();
        }

        @BeforeEach
        void before() throws URISyntaxException {
            webSocketClient = new WebSocketTestClient(new URI("wss://localhost:" + port + "/websocketservice/ws/v1"));
            webSocketClient.setSocketFactory(apimlSSLContext.getSocketFactory());
            webSocketClient.messages.clear();
        }

        @AfterEach
        void tearDown() throws InterruptedException {
            if (webSocketClient.isOpen() && !webSocketClient.isClosed() && !webSocketClient.isClosing()) {
                webSocketClient.closeBlocking();
            }
        }

        @Test
        void whenConnectingToWrongCert_thenSucceed() throws URISyntaxException, InterruptedException {
            var connected = webSocketClient.connectBlocking();
            webSocketClient.setOnClose((status, reason) -> {
                assertFalse(reason != null && reason.contains("No name matching localhost found"));
            });

            assertTrue(connected);

            var frame = new TextFrame();
            frame.setPayload(ByteBuffer.wrap("null".getBytes()));
            webSocketClient.sendFrame(frame);

            await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    var messages = webSocketClient.getMessages();
                    assertEquals(1, messages.size());
                    assertEquals("ACK:null", messages.get(0));
                });
        }

    }

    @MicroservicesAcceptanceTest
    @TestInstance(Lifecycle.PER_CLASS)
    @ActiveProfiles({ "WebSocketTest" })
    @TestPropertySource(
        properties = {
            "spring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=3145728" // bytes
        }
    )
    @Slf4j
    @Nested
    class OnStrictHostname extends AcceptanceTestWithMockServices {

        private WebSocketTestClient webSocketClient;
        private WebSocketTestClient webSocketClientTyrus;
        private WebSocketTestClient webSocketClientStrict;

        private MockService serviceStrictness;

        @BeforeAll
        void setUp() {
            var service1 = mockServiceWs("websocketservice")
                .assertion(message -> {
                    if (message instanceof String s) {
                        assertTrue(StringUtils.isNotBlank(s));
                    } else if (message instanceof ByteBuffer b) {
                        assertTrue(b.remaining() > 0);
                    }
                })
                .scope(Scope.CLASS)
                .start();

            var service2 = mockServiceWsTyrus("tyrusws") // To test response with frames
                .scope(Scope.CLASS)
                .start();

            serviceStrictness = mockServiceWs("websocketservicessl")
                .sslContext(apimlNonStrictSSLContext) // To verify the non strict hostname restriction works
                .scope(Scope.CLASS)
                .assertion(message -> {
                    if (message instanceof String s) {
                        assertTrue(StringUtils.isNotBlank(s));
                    } else if (message instanceof ByteBuffer b) {
                        assertTrue(b.remaining() > 0);
                    }
                })
                .start();

            assertEquals(MockService.Status.STARTED, service1.getStatus());
            assertEquals(MockService.Status.STARTED, service2.getStatus());
            assertEquals(MockService.Status.STARTED, serviceStrictness.getStatus());
        }

        @BeforeEach
        void before() throws URISyntaxException {
            webSocketClient = new WebSocketTestClient(new URI("wss://localhost:" + port + "/websocketservice/ws/v1"));
            webSocketClient.setSocketFactory(apimlSSLContext.getSocketFactory());
            webSocketClient.messages.clear();

            webSocketClientTyrus = new WebSocketTestClient(new URI("wss://localhost:" + port + "/tyrusws/ws/v1/echo"));
            webSocketClientTyrus.setSocketFactory(apimlSSLContext.getSocketFactory());
            webSocketClientTyrus.messages.clear();

            webSocketClientStrict = new WebSocketTestClient(new URI("wss://localhost:" + port + "/websocketservicessl/ws/v1"));
            webSocketClientStrict.setSocketFactory(apimlSSLContext.getSocketFactory());
            webSocketClientStrict.messages.clear();
        }

        @AfterEach
        void tearDown() throws InterruptedException {
            if (webSocketClient.isOpen() && !webSocketClient.isClosed() && !webSocketClient.isClosing()) {
                webSocketClient.closeBlocking();
            }
            if (webSocketClientTyrus.isOpen() && !webSocketClientTyrus.isClosed() && !webSocketClientTyrus.isClosing()) {
                webSocketClientTyrus.closeBlocking();
            }
            if (webSocketClientStrict.isOpen() && !webSocketClientStrict.isClosed() && !webSocketClientStrict.isClosing()) {
                webSocketClientStrict.closeBlocking();
            }
        }

        @Test
        void givenWsConnection_withSingleMessage_thenSuccess() throws InterruptedException {
            var connected = webSocketClient.connectBlocking();

            assertTrue(connected);

            var frame = new TextFrame();
            frame.setPayload(ByteBuffer.wrap("null".getBytes()));
            webSocketClient.sendFrame(frame);

            await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> {
                    var messages = webSocketClient.getMessages();
                    assertEquals(1, messages.size());
                    assertEquals("ACK:null", messages.get(0));
                });
        }

        @Test
        void whenConnectToServerWithWrongCert_thenReject() throws URISyntaxException, InterruptedException {
            webSocketClientStrict.connectBlocking();
            var reasonValid = new AtomicBoolean(false);
            webSocketClientStrict.setOnClose((status, reason) -> {
                assertEquals(1011, status);
                assertNotNull(reason);
                // assertTrue(reason.contains("No name matching localhost found"), "reason was: " + reason); On Netty client
                assertTrue(reason.contains("HTTP request to initiate the WebSocket connection to [wss://localhost:" + serviceStrictness.getPort() + "/websocketservicessl/ws] failed")); // On Tomcat client
                reasonValid.set(true);
            });

            await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertTrue(reasonValid.get());
                });
        }

        @Test
        void givenWsConnection_withFramedMessage_thenSuccess() throws InterruptedException {
            var connected = webSocketClient.connectBlocking();

            assertTrue(connected);

            webSocketClient.sendFragmentedFrame(Opcode.TEXT, ByteBuffer.wrap("AB".getBytes()), false);
            webSocketClient.sendFragmentedFrame(Opcode.TEXT, ByteBuffer.wrap("CD".getBytes()), true);

            await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    var messages = webSocketClient.getMessages();
                    assertEquals(1, messages.size());
                    assertEquals("ACK:ABCD", messages.get(0));
                });
        }

        @Test
        void whenTyrusBinaryFrameResponse_thenSucceed() throws InterruptedException {
            var connected = webSocketClientTyrus.connectBlocking();
            assertTrue(connected);

            var data = RandomUtils.insecure().randomBytes(21_504);
            var frame1 = ByteBuffer.wrap(ArrayUtils.subarray(data, 0, 8192));
            var frame2 = ByteBuffer.wrap(ArrayUtils.subarray(data, 8192, 16384));
            var frame3 = ByteBuffer.wrap(ArrayUtils.subarray(data, 16384, 21504));

            webSocketClientTyrus.sendFragmentedFrame(Opcode.BINARY, frame1, false);
            webSocketClientTyrus.sendFragmentedFrame(Opcode.BINARY, frame2, false);
            webSocketClientTyrus.sendFragmentedFrame(Opcode.BINARY, frame3, true);

            await()
                .atMost(Duration.ofSeconds(40))
                .untilAsserted(() -> {
                    var response = webSocketClientTyrus.getBuffer();
                    assertNotNull(response);
                    assertTrue(response.remaining() > 0);
                    assertTrue(response.capacity() == 21_504, "capacity was " + response.capacity());
                });
        }

    }

    private static class WebSocketTestClient extends WebSocketClient {

        @Getter
        private List<String> messages = new ArrayList<>();

        @Getter
        private ByteBuffer buffer;

        @Setter
        private BiConsumer<Integer, String> onClose;

        public WebSocketTestClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            messages.clear();
        }

        @Override
        public void onMessage(String message) {
            messages.add(message);
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            buffer = bytes;
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (onClose != null) {
                onClose.accept(code, reason);
            }
        }

        @Override
        public void onError(Exception ex) {
            fail(ex);
        }

    }

}

@TestConfiguration
@Profile("WebSocketTest")
class WebSocketTestConfiguration {

}
