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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    properties = {
        "server.webSocket.maxIdleTimeout=10000",
        "server.webSocket.connectTimeout=1000",
        "server.webSocket.stopTimeout=500",
        "server.webSocket.asyncWriteTimeout=1500",
        "server.webSocket.requestBufferSize=9090"
    },
    classes = { WebSocketClientFactory.class }
)
@MockBean(SslContextFactory.Client.class)
@ActiveProfiles("WebSocketClientFactoryContextTest")
public class WebSocketClientFactoryContextTest {

    @Autowired
    private WebSocketClientFactory webSocketClientFactory;

    @BeforeEach
    void setUp() {
        webSocketClientFactory.getClientInstance("key");
    }

    @Nested
    class GivenWebSocketClientParametrization {

        @Test
        void thenBeanIsInitialized() {
            assertNotNull(webSocketClientFactory);

            JettyWebSocketClient jettyWebSocketClient = webSocketClientFactory.getClientInstance("key");
            WebSocketClient webSocketClient = (WebSocketClient) ReflectionTestUtils.getField(jettyWebSocketClient, "client");

            WebSocketPolicy policy = webSocketClient.getPolicy();
            HttpClient httpClient = webSocketClient.getHttpClient();

            assertEquals(10000, policy.getIdleTimeout());
            assertEquals(1000, httpClient.getConnectTimeout());
            assertEquals(500, webSocketClient.getStopTimeout());
            assertEquals(1500, policy.getAsyncWriteTimeout());
            assertEquals(9090, httpClient.getRequestBufferSize());
        }

    }

    @Nested
    class GivenFactory {

        private JettyWebSocketClient client = mock(JettyWebSocketClient.class);
        private WebSocketClientFactory factory = new WebSocketClientFactory(null, 0, 0, 0, 0, 0);

        @BeforeEach
        void setUp() {
            ConcurrentMap<String, JettyWebSocketClient> clients = new ConcurrentHashMap<>();
            clients.put("key", client);
            ReflectionTestUtils.setField(factory, "clientsMap", clients);
        }

        @Test
        void whenIsRunning_thenStop() {
            doReturn(true).when(client).isRunning();
            factory.closeClients();
            verify(client).stop();
        }

        @Test
        void whenIsNotRunning_thenDontStop() {
            factory.closeClients();
            verify(client, never()).stop();
        }

    }

}
