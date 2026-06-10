/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketClientFactoryTest {

    @Nested
    class CreatedInstance {

        @Mock
        private JettyWebSocketClient client;

        private WebSocketClientFactory webSocketClientFactory;

        @BeforeEach
        void setUp() {
            this.webSocketClientFactory = new WebSocketClientFactory(null, 0, 0, 0, 0, 0);
            ConcurrentMap<String, JettyWebSocketClient> clients = new ConcurrentHashMap<>();
            clients.put("key", client);
            ReflectionTestUtils.setField(webSocketClientFactory, "clientsMap", clients);
        }

        @Test
        void givenRunningClient_whenClose_thenStopClient() {
            doReturn(true).when(client).isRunning();
            webSocketClientFactory.closeClients();
            verify(client).stop();
        }

        @Test
        void givenStoppedClient_whenClose_thenDoNothing() {
            webSocketClientFactory.closeClients();
            verify(client, never()).stop();
        }

        @Test
        void whenGetClient_thenReturnInstance() {
            assertSame(client, webSocketClientFactory.getClientInstance("key"));
        }

    }

    @Nested
    class CreatedInstanceWithConfig {

        private WebSocketClientFactory webSocketClientFactory;

        @BeforeEach
        void setUp() {
            SslContextFactory.Client sslClient = mock(SslContextFactory.Client.class);
            this.webSocketClientFactory = new WebSocketClientFactory(sslClient, 1234, 0, 0, 0, 0);
        }

        @Test
        void givenInitilizedClient_thenHasNonDefaultIdleConfig() {
            WebSocketClient wsClient = (WebSocketClient) ReflectionTestUtils.getField(webSocketClientFactory.getClientInstance("key"), "client");
            assertEquals(1234, wsClient.getMaxIdleTimeout());
        }

    }

}
