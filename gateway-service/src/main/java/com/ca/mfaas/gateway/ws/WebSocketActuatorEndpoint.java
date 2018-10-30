/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Component
@Endpoint(id = "websockets")
public class WebSocketActuatorEndpoint {
    private WebSocketProxyServerHandler webSocketProxyServerHandler;

    @Autowired
    public WebSocketActuatorEndpoint(WebSocketProxyServerHandler webSocketProxyServerHandler) {
        this.webSocketProxyServerHandler = webSocketProxyServerHandler;
    }

    @ReadOperation
    public List<Map<String, String>> getAll() {
        List<Map<String, String>> result = new ArrayList<>();

        for (Entry<String, WebSocketRoutedSession> entry : webSocketProxyServerHandler.getRoutedSessions().entrySet()) {
            Map<String, String> map = new LinkedHashMap<>();

            map.put("sessionId", entry.getKey());
            map.put("clientAddress", entry.getValue().getWebSocketServerSession().getRemoteAddress().toString());
            map.put("gatewayPath", entry.getValue().getWebSocketServerSession().getUri().toString());
            map.put("serviceUrl", entry.getValue().getWebSocketClientSession().getUri().toString());
            map.put("serviceSessionId", entry.getValue().getWebSocketClientSession().getId());

            result.add(map);
        }

        return result;
    }
}
