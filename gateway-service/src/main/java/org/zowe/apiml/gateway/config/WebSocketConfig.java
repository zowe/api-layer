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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.zowe.apiml.gateway.websocket.ApimlRequestUpgradeStrategy;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.common.util.ConnectionUtil;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private static final ApimlLogger apimlLog = ApimlLogger.empty();
    private final HttpClientProperties httpClientProperties;

    @Bean
    @Primary
    WebSocketClient webSocketClient(HttpConfig config, HttpClient httpClient) {
        HttpClient secureClient;
        try {
            secureClient = ConnectionUtil.getHttpClient(config, httpClient, false);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config.httpsConfig());
        }
        var spec = WebsocketClientSpec.builder()
            .handlePing(httpClientProperties.getWebsocket().isProxyPing());

        //Set the gateway outbound frame limit for websockets
        var maxFramePayloadLength = httpClientProperties.getWebsocket().getMaxFramePayloadLength();
        if (maxFramePayloadLength != null) {
            spec.maxFramePayloadLength(httpClientProperties.getWebsocket().getMaxFramePayloadLength());
        }

        var client = new ReactorNettyWebSocketClient(secureClient, () -> spec);
        return client;
    }

    @Bean
    @Primary
    RequestUpgradeStrategy requestUpgradeStrategy() {
        return new ApimlRequestUpgradeStrategy(httpClientProperties);
    }

}
