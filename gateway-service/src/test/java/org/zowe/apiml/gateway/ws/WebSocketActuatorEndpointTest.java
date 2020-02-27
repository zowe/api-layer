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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.sockjs.transport.session.WebSocketServerSockJsSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketActuatorEndpointTest {

    private WebSocketActuatorEndpoint webSocketActuatorEndpoint;
    private WebSocketProxyServerHandler webSocketProxyServerHandler;
    private WebSocketRoutedSession webSocketRoutedSession;
    private WebSocketServerSockJsSession webSocketServerSockJsSession;
    private WebSocketSession session;

    @BeforeEach
    public void setup() {
        webSocketProxyServerHandler = mock(WebSocketProxyServerHandler.class);
        session = mock(WebSocketSession.class);
        webSocketRoutedSession = mock(WebSocketRoutedSession.class);
        webSocketServerSockJsSession = mock(WebSocketServerSockJsSession.class);

        webSocketActuatorEndpoint = new WebSocketActuatorEndpoint(webSocketProxyServerHandler);
    }

    @Test
    public void should() throws Exception {

        URI uri = new URI("ws://localhost:8080/abc");

        Map<String, WebSocketRoutedSession> routedSessions = new ConcurrentHashMap<>();

        when(webSocketRoutedSession.getWebSocketServerSession()).thenReturn(webSocketServerSockJsSession);
        when(webSocketRoutedSession.getWebSocketClientSession()).thenReturn(session);
        when(webSocketRoutedSession.getWebSocketServerSession().getRemoteAddress()).thenReturn(new InetSocketAddress(80));
        when(webSocketRoutedSession.getWebSocketServerSession().getUri()).thenReturn(uri);
        when(webSocketRoutedSession.getWebSocketClientSession().getUri()).thenReturn(uri);

        routedSessions.put("websocket", webSocketRoutedSession);

        when(webSocketProxyServerHandler.getRoutedSessions()).thenReturn(routedSessions);

        List<Map<String, String>> expectedResult = getMockedResultMaps();

        List<Map<String, String>> result = webSocketActuatorEndpoint.getAll();

        assertEquals(expectedResult, result);
    }

    private List<Map<String, String>> getMockedResultMaps() {

        List<Map<String, String>> expectedResult = new ArrayList<>();
        Map<String, String> map = new LinkedHashMap<>();

        map.put("sessionId", "websocket");
        map.put("clientAddress", "0.0.0.0/0.0.0.0:80");
        map.put("gatewayPath", "ws://localhost:8080/abc");
        map.put("serviceUrl", "ws://localhost:8080/abc");
        map.put("serviceSessionId", null);

        expectedResult.add(map);
        return expectedResult;
    }

}
