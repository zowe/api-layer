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

    public ApimlWebSocketClient(SSLContext sslContext, WsWebSocketContainer webSocketContainer) {
        super(webSocketContainer);
        this.sslContext = sslContext;
    }

    @Override
    protected ClientEndpointConfig createEndpointConfig(Configurator configurator, List<String> subProtocols) {
        return ClientEndpointConfig.Builder.create()
                .configurator(configurator)
                .preferredSubprotocols(subProtocols)
                .sslContext(sslContext)
                .build();
    }

}
