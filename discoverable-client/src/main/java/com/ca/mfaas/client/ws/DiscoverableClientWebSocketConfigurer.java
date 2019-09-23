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

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
@Component
@SuppressWarnings("squid:S1075")
public class DiscoverableClientWebSocketConfigurer implements WebSocketConfigurer {

    @InjectApimlLogger
    private ApimlLogger logger = ApimlLogger.empty();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String webSocketPath = "/ws/uppercase";

        logger.log("com.ca.mfaas.log.sampleservice.registeringWebSocket", webSocketPath);

        registry.addHandler(new WebSocketServerHandler(), webSocketPath).setAllowedOrigins("*");

        webSocketPath = "/ws/header";
        logger.log("com.ca.mfaas.log.sampleservice.registeringWebSocket", webSocketPath);

        registry.addHandler(new HeaderSocketServerHandler(), webSocketPath);
    }
}
