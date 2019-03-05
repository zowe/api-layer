/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.gateway.ws;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Component
public class GatewayWebSocketConfigurer implements WebSocketConfigurer {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GatewayWebSocketConfigurer.class);
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
