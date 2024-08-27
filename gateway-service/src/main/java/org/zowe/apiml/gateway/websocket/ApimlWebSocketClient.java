/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.Constants;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;
import org.springframework.web.reactive.socket.client.StandardWebSocketClient;
import reactor.core.publisher.Sinks;

import java.util.List;

@Slf4j
public class ApimlWebSocketClient extends StandardWebSocketClient {

    @Value("${server.webSocket.connectTimeout:45000}")
    private String ioTimeout = "45000";

    public ApimlWebSocketClient(WsWebSocketContainer webSocketContainer) {
        super(webSocketContainer);
    }

    @Override
    protected StandardWebSocketSession createWebSocketSession(
        Session session, HandshakeInfo info, Sinks.Empty<Void> completionSink) {
        return new ApimlWebSocketSession(session, info, bufferFactory(), completionSink);
    }

    @Override
    protected ClientEndpointConfig createEndpointConfig(ClientEndpointConfig.Configurator configurator, List<String> subProtocols) {
        var config = ClientEndpointConfig.Builder.create()
            .configurator(configurator)
            .preferredSubprotocols(subProtocols)
            .build();
        config.getUserProperties().put(Constants.IO_TIMEOUT_MS_PROPERTY, ioTimeout);
        return config;
    }

}
