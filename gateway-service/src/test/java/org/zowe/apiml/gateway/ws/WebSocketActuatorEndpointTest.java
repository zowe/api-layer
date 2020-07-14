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

import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketActuatorEndpointTest {

    private WebSocketActuatorEndpoint underTest;
    private WebSocketProxyServerHandler webSocketProxyServerHandler;

    @BeforeEach
    public void setup() {
        webSocketProxyServerHandler = mock(WebSocketProxyServerHandler.class);
        underTest = new WebSocketActuatorEndpoint(webSocketProxyServerHandler);
    }

    @Test
    void givenExistingRoute_whenTheStatusOfRoutesIsRequested_thenTheListIsReturned() {
        Map<String, WebSocketRoutedSession> routedSessions = new HashMap<>();
        WebSocketRoutedSession validSession = mock(WebSocketRoutedSession.class);
        when(validSession.getClientId()).thenReturn("12");
        when(validSession.getClientUri()).thenReturn("ws://localhost:8080/v2");
        when(validSession.getServerUri()).thenReturn("ws://gateway:10010/api/v2/");
        when(validSession.getServerRemoteAddress()).thenReturn("ws://gateway:10010");
        routedSessions.put("webSocketSessionId", validSession);
        when(webSocketProxyServerHandler.getRoutedSessions()).thenReturn(routedSessions);

        String clientResponse = new JSONArray(underTest.getAll()).toString();
        assertThat(clientResponse, is("[{\"gatewayPath\":\"ws:\\/\\/gateway:10010\\/api\\/v2\\/\",\"serviceUrl\":\"ws:\\/\\/localhost:8080\\/v2\",\"serviceSessionId\":\"12\",\"sessionId\":\"webSocketSessionId\",\"clientAddress\":\"ws:\\/\\/gateway:10010\"}]"));
    }
}
