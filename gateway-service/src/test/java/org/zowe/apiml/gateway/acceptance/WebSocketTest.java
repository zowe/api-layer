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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.Opcode;
import org.java_websocket.framing.TextFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@MicroservicesAcceptanceTest
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles({ "WebSocketTest" })
@TestPropertySource(
    properties = {
        "spring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=100" // bytes
    }
)
@Slf4j
class WebSocketTest extends AcceptanceTestWithMockServices {

    @SuppressWarnings("unused")
    private MockService mockServiceWs;

    private WebSocketTestClient webSocketClient;

    @BeforeAll
    void setUp() throws URISyntaxException {
        mockServiceWs = mockServiceWs("websocketservice")
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
    @Disabled
    void givenWsConnection_withSingleMessage_thenSuccess() throws URISyntaxException, InterruptedException {
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
    @Disabled
    void givenWsConnection_withFramedMessage_thenSuccess() throws InterruptedException {
        var connected = webSocketClient.connectBlocking();

        assertTrue(connected);

        webSocketClient.sendFragmentedFrame(Opcode.TEXT, ByteBuffer.wrap("AB".getBytes()), false);
        webSocketClient.sendFragmentedFrame(Opcode.TEXT, ByteBuffer.wrap("CD".getBytes()), true);

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                var messages = webSocketClient.getMessages();
                assertEquals(1, messages.size());
                assertEquals("ACK:ABCD", messages.get(0));

            });
    }

    @Test
    void testBinary() throws InterruptedException {
        var connected = webSocketClient.connectBlocking();

        assertTrue(connected);

        var data = RandomUtils.insecure().randomBytes(90);
        var frame1 = ByteBuffer.wrap(ArrayUtils.subarray(data, 0, 40));
        var frame2 = ByteBuffer.wrap(ArrayUtils.subarray(data, 40, 80));
        var frame3 = ByteBuffer.wrap(ArrayUtils.subarray(data, 80, 90));

        webSocketClient.sendFragmentedFrame(Opcode.BINARY, frame1, false);
        webSocketClient.sendFragmentedFrame(Opcode.BINARY, frame2, false);
        webSocketClient.sendFragmentedFrame(Opcode.BINARY, frame3, true);

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                var messages = webSocketClient.getMessages();
                assertEquals(1, messages.size());
                assertEquals("ACK:90", messages.get(0));
            });
    }

    @Test
    void givenWsConnection_withFramedMessageOverLimit_thenSucess() throws InterruptedException {
        var connected = webSocketClient.connectBlocking();

        assertTrue(connected);

        var fullMessage = RandomStringUtils.insecure().next(500);

        var ackMessage = "ACK:" + fullMessage;

        webSocketClient.sendFragmentedFrame(null, null, connected);
    }



    private static class WebSocketTestClient extends WebSocketClient {

        @Getter
        private List<String> messages = new ArrayList<>();

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
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("closed");
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
