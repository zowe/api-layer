/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;

class WebSocketClientFactoryTest {

    @Nested
    class CreatedInstance {

        private WebSocketClientFactory webSocketClientFactory;
        private JettyWebSocketClient client;

        @BeforeEach
        void setUp() {
            this.client = mock(JettyWebSocketClient.class);
            this.webSocketClientFactory = new WebSocketClientFactory(this.client);
        }

        @Test
        void givenRunningClient_whenClose_thenStopClient() {
            doReturn(true).when(client).isRunning();
            webSocketClientFactory.closeClient();
            verify(client).stop();
        }

        @Test
        void givenStoppedClient_whenClose_thenDoNothing() {
            webSocketClientFactory.closeClient();
            verify(client, never()).stop();
        }

        @Test
        void whenGetClient_thenReturnInstance() {
            assertSame(client, webSocketClientFactory.getClientInstance());
        }

    }

    @Nested
    class CreatedInstanceWithConfig {

        private WebSocketClientFactory webSocketClientFactory;

        @BeforeEach
        void setUp() {
            SslContextFactory.Client sslClient = mock(SslContextFactory.Client.class);
            this.webSocketClientFactory = new WebSocketClientFactory(sslClient, 1234);
        }

        @Test
        void givenInitilizedClient_thenHasNonDefaultIdleConfig() {
            WebSocketClient wsClient = (WebSocketClient) ReflectionTestUtils.getField(webSocketClientFactory.getClientInstance(), "client");
            assertEquals(1234, wsClient.getMaxIdleTimeout());
        }

    }

}
