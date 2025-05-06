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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.zowe.apiml.gateway.websocket.ApimlRequestUpgradeStrategy;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

@Slf4j
@Configuration
public class WebSocketConfig {

    @Bean
    @Primary
    WebSocketClient webSocketClient(ConnectionsConfig connectionsConfig, HttpClient httpClient) {
        var secureClient = connectionsConfig.getHttpClient(httpClient, false);
        var spec = WebsocketClientSpec.builder()
            .handlePing(true);

        var client = new ReactorNettyWebSocketClient(secureClient, () -> spec);
        return client;
    }

    @Bean
    @Primary
    RequestUpgradeStrategy requestUpgradeStrategy() {
        return new ApimlRequestUpgradeStrategy();
    }

}
