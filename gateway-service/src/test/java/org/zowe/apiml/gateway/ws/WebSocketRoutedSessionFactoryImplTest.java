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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketRoutedSessionFactoryImplTest {

    private static final String WSS_URI = "wss://service";

    @Mock
    private WebSocketClientFactory factory;
    @Mock
    private WebSocketSession webSocketSession;
    @Mock
    private JettyWebSocketClient jettyWebSocketClient;

    private WebSocketRoutedSessionFactoryImpl underTest = new WebSocketRoutedSessionFactoryImpl();

    @BeforeEach
    void setUp() {
        when(webSocketSession.getHandshakeHeaders()).thenReturn(HttpHeaders.EMPTY);
    }

    @Test
    void testSession() {
        when(factory.getClientInstance(WSS_URI)).thenReturn(jettyWebSocketClient);
        when(jettyWebSocketClient.doHandshake(any(), any(WebSocketHttpHeaders.class), any(URI.class))).thenReturn(AsyncResult.forValue(null));

        WebSocketRoutedSession generated = underTest.session(webSocketSession, WSS_URI, factory, null, null);

        assertNotNull(generated);
    }
}
