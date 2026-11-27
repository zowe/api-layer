/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.apache.http.client.utils.URIBuilder;
import org.apache.tomcat.websocket.Constants;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.WebsocketTest;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_WS_PARTIAL_ECHO;

/** Verifies the default forwarding mode against Discoverable Client without service metadata overrides. */
@WebsocketTest
@DisabledIfSystemProperty(named = "environment.websocket.partial", matches = "false")
@Timeout(60)
class PartialWebSocketProxyTest implements TestWithStartedInstances {
    private static final int WAIT_SECONDS = 10;
    private final BlockingQueue<Part> received = new LinkedBlockingQueue<>();
    private final WsWebSocketContainer container = new WsWebSocketContainer();
    private Session client;
    private volatile String transportFailure;

    private record Part(byte[] data, boolean binary, boolean last) { }

    private void connect() throws Exception {
        var gateway = HttpRequestUtils.getUriFromGateway(DISCOVERABLE_WS_PARTIAL_ECHO);
        var uri = new URIBuilder(gateway).setScheme("https".equals(gateway.getScheme()) ? "wss" : "ws").build();
        var config = ClientEndpointConfig.Builder.create().sslContext(HttpClientUtils.ignoreSslContext())
            .configurator(new ClientEndpointConfig.Configurator() {
                @Override
                public void beforeRequest(Map<String, List<String>> headers) {
                    String credentials = Base64.getEncoder().encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
                    headers.put("Authorization", List.of("Basic " + credentials));
                }
            }).build();
        config.getUserProperties().put(Constants.IO_TIMEOUT_MS_PROPERTY, "10000");
        config.getUserProperties().put(Constants.BLOCKING_SEND_TIMEOUT_PROPERTY, 10_000L);
        client = container.connectToServer(new Endpoint() {
            @Override
            public void onOpen(Session session, EndpointConfig endpointConfig) {
                // Split receive callbacks without shrinking Tomcat's TLS handshake response buffer.
                session.setMaxTextMessageBufferSize(1024);
                session.setMaxBinaryMessageBufferSize(1024);
                session.addMessageHandler(String.class, (MessageHandler.Partial<String>) (data, last) ->
                    received.add(new Part(data.getBytes(StandardCharsets.UTF_8), false, last)));
                session.addMessageHandler(ByteBuffer.class, (MessageHandler.Partial<ByteBuffer>) (data, last) -> {
                    byte[] copy = new byte[data.remaining()];
                    data.get(copy);
                    received.add(new Part(copy, true, last));
                });
            }

            @Override
            public void onError(Session session, Throwable error) {
                transportFailure = error.toString();
            }

            @Override
            public void onClose(Session session, CloseReason reason) {
                transportFailure = reason.toString();
            }
        }, config, uri);
    }

    @AfterEach
    void close() throws Exception {
        try {
            if (client != null && client.isOpen()) client.close();
        } finally {
            container.destroy();
        }
    }

    @Test
    void forwardsTextInBothDirectionsBeforeFin() throws Exception {
        connect();
        String first = "Hello € 🚀 ".repeat(200);
        client.getBasicRemote().sendText(first, false);
        // FIN is deliberately withheld until its echo has traversed both gateway connections.
        assertEcho(first.getBytes(StandardCharsets.UTF_8), false, false);
        client.getBasicRemote().sendText("second part", false);
        assertEcho("second part".getBytes(StandardCharsets.UTF_8), false, false);
        client.getBasicRemote().sendText("", true);
        assertEcho(new byte[0], false, true);

        client.getBasicRemote().sendText("next message", true);
        assertEcho("next message".getBytes(StandardCharsets.UTF_8), false, true);
    }

    @Test
    void forwardsBinaryInBothDirectionsBeforeFin() throws Exception {
        connect();
        byte[] first = new byte[4096];
        for (int i = 0; i < first.length; i++) first[i] = (byte) (i * 31);
        client.getBasicRemote().sendBinary(ByteBuffer.wrap(first), false);
        assertEcho(first, true, false);
        byte[] second = {0, 1, (byte) 0x80, (byte) 0xff};
        client.getBasicRemote().sendBinary(ByteBuffer.wrap(second), false);
        assertEcho(second, true, false);
        client.getBasicRemote().sendBinary(ByteBuffer.allocate(0), true);
        assertEcho(new byte[0], true, true);

        client.getBasicRemote().sendBinary(ByteBuffer.wrap(second), true);
        assertEcho(second, true, true);
    }

    private void assertEcho(byte[] expected, boolean binary, boolean last) throws Exception {
        var actual = new ByteArrayOutputStream();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        Part part;
        do {
            part = received.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            assertNotNull(part, () -> "No incremental echo through the gateway. Check partial forwarding is enabled "
                + "on the gateway and in Discoverable Client metadata. Transport failure: " + transportFailure);
            assertEquals(binary, part.binary(), "The WebSocket message type must be preserved");
            actual.writeBytes(part.data());
            assertTrue(actual.size() <= expected.length, "Unexpected data or message boundary");
            if (!last) assertFalse(part.last(), "A nonfinal source part must not complete the echoed message");
            if (part.last()) break;
        } while (last || actual.size() < expected.length);
        assertArrayEquals(expected, actual.toByteArray());
        assertEquals(last, part.last(), "The message must complete only when the source sends FIN");
    }
}
