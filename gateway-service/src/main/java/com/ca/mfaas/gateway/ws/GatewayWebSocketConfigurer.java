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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Component
public class GatewayWebSocketConfigurer implements WebSocketConfigurer {
    private WebSocketProxyServerHandler webSocketProxyServerHandler;

    @Autowired
    public GatewayWebSocketConfigurer(WebSocketProxyServerHandler webSocketProxyServerHandler) {
        this.webSocketProxyServerHandler = webSocketProxyServerHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String webSocketPath = "/ws/**";
        log.debug("Registering WebSocket proxy handler to " + webSocketPath);
        registry.addHandler(webSocketProxyServerHandler, webSocketPath);
    }
}
