/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.ws;

import com.ca.mfaas.message.core.MessageType;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Component
public class DiscoverableClientWebSocketConfigurer implements WebSocketConfigurer {

    @InjectApimlLogger
    private ApimlLogger logger = ApimlLogger.empty();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String webSocketEndpoint = "/ws/uppercase";

        logger.log("com.ca.mfaas.log.sampleservice.registeringWebSocket", webSocketEndpoint);

        registry.addHandler(new WebSocketServerHandler(), webSocketEndpoint).setAllowedOrigins("*");

        webSocketEndpoint = "/ws/header";
        logger.log(MessageType.DEBUG, "Registering WebSocket handler to {}", webSocketEndpoint);

        registry.addHandler(new HeaderSocketServerHandler(), webSocketEndpoint);
    }
}
