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
import jakarta.websocket.ClientEndpointConfig.Configurator;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.web.reactive.socket.client.TomcatWebSocketClient;

import javax.net.ssl.SSLContext;

import java.util.List;

public class ApimlWebSocketClient extends TomcatWebSocketClient {

    private final SSLContext sslContext;
    private final WebSocketProperties properties;

    public ApimlWebSocketClient(SSLContext sslContext, WsWebSocketContainer webSocketContainer) {
        this(sslContext, webSocketContainer, new WebSocketProperties());
    }

    public ApimlWebSocketClient(SSLContext sslContext, WsWebSocketContainer webSocketContainer,
        WebSocketProperties properties) {
        super(webSocketContainer);
        this.sslContext = sslContext;
        this.properties = properties;
    }

    @Override
    protected ClientEndpointConfig createEndpointConfig(Configurator configurator, List<String> subProtocols) {
        var config = ClientEndpointConfig.Builder.create()
                .configurator(configurator)
                .preferredSubprotocols(subProtocols)
                .sslContext(sslContext)
                .build();
        config.getUserProperties().put(org.apache.tomcat.websocket.Constants.IO_TIMEOUT_MS_PROPERTY,
            Long.toString(properties.getConnectTimeout()));
        return config;
    }

}
