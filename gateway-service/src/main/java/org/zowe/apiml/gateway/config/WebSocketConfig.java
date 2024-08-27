/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.zowe.apiml.gateway.websocket.ApimlRequestUpgradeStrategy;
import org.zowe.apiml.gateway.websocket.ApimlWebSocketClient;

@Configuration
public class WebSocketConfig {

    @Value("${server.webSocket.requestBufferSize:16000000}")
    private int bufferSize;
    @Value("${server.webSocket.asyncWriteTimeout:60000}")
    private long sendTimeout;
    @Value("${server.webSocket.maxIdleTimeout:3600000}")
    private long idleTimeout;

    @Bean
    @Primary
    public WebSocketClient tomcatWebSocketClient() {
        var wsContainer = new WsWebSocketContainer();
        wsContainer.setDefaultMaxTextMessageBufferSize(bufferSize);
        wsContainer.setDefaultMaxBinaryMessageBufferSize(bufferSize);
        wsContainer.setAsyncSendTimeout(sendTimeout);
        wsContainer.setDefaultMaxSessionIdleTimeout(idleTimeout);
        return new ApimlWebSocketClient(wsContainer);
    }

    @Bean
    @Primary
    public RequestUpgradeStrategy requestUpgradeStrategy() {
        return new ApimlRequestUpgradeStrategy();
    }

}
