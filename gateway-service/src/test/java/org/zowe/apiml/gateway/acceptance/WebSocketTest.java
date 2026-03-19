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
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.TextFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeAll;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.awaitility.Awaitility.await;

@MicroservicesAcceptanceTest
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles({ "WebSocketTest" })
@TestPropertySource(
    properties = {
        "spring.cloud.gateway.server.webflux.httpclient.websocket.max-frame-payload-length=1000"
    }
)
@Slf4j
class WebSocketTest extends AcceptanceTestWithMockServices {

    @SuppressWarnings("unused")
    private MockService mockServiceWs;

    @BeforeAll
    void setUp() {
        mockServiceWs = mockServiceWs("websocketservice")
            .assertion(message -> assertTrue(StringUtils.isNotBlank(message)))
            .start();
    }

    @Test
    void givenWsConnection_withFragmentedMessages_thenSuccess() throws URISyntaxException, InterruptedException {
        var client = new WebSocketTestClient(new URI("wss://localhost:" + port + "/websocketservice/ws/v1"));

        var connected = client.connectBlocking();

        assertTrue(connected);

        var frame = new TextFrame();
        frame.setPayload(ByteBuffer.wrap("null".getBytes()));
        client.sendFrame(frame);

        await()
            .untilAsserted(() -> {
                var messages = client.getMessages();
                assertEquals(1, messages.size());
                assertEquals("ACK", messages.get(0));
            });
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

        }

        @Override
        public void onError(Exception ex) {

        }

    }

}

@TestConfiguration
@Profile("WebSocketTest")
class WebSocketTestConfiguration {

}
