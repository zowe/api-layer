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

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.zowe.apiml.util.SecurityUtils;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_STOMP;

@Disabled
public class StompProxyTest extends WebSocketProxyTest {

    private static final String SEND_ENDPOINT = "/app/replyWithSameSize/";
    private static final String SUBSCRIBE_ENDPOINT = "/topic/replyWithSameSize/";

    private static WebSocketStompClient stompClient;

    private CompletableFuture<String> completableFuture;

    @BeforeAll
    public static void setUpStompClient() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(3 * 1024 * 1024);
        container.setDefaultMaxBinaryMessageBufferSize(3 * 1024 * 1024);

        var standardWebSocketClient = new StandardWebSocketClient(container);
        standardWebSocketClient.setSslContext(SecurityUtils.getSslContext());

        stompClient = new WebSocketStompClient(standardWebSocketClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        stompClient.setInboundMessageSizeLimit(Integer.MAX_VALUE);
        stompClient.setOutboundMessageSizeLimit(Integer.MAX_VALUE);
    }

    @Test
    void stompOverWebsocketLargeMessageExchange() throws Exception {
        completableFuture = new CompletableFuture<>();
        String uuid = UUID.randomUUID().toString();

        StompSession stompSession = stompClient.connectAsync(
            discoverableClientGatewayUrl(DISCOVERABLE_STOMP), VALID_AUTH_HEADERS, new StompSessionHandlerAdapter() {
        }).get(5, SECONDS); // lower connection timeout fails on z/os test system
        stompSession.subscribe(SUBSCRIBE_ENDPOINT + uuid, new StringStompFrameHandler());

        char c = 'A';
        int payloadSize = 1024 * 1024;
        stompSession.send(SEND_ENDPOINT + uuid, String.valueOf(c).repeat(payloadSize));

        String response = completableFuture.get(10, SECONDS);
        stompSession.disconnect();

        assertEquals(payloadSize, response.getBytes().length);
    }

    private class StringStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            completableFuture.complete((String) o);
        }
    }
}
