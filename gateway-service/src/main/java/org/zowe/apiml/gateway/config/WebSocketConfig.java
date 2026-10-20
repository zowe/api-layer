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
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.zowe.apiml.gateway.websocket.ApimlRequestUpgradeStrategy;
import org.zowe.apiml.gateway.websocket.ApimlWebSocketClient;
import org.zowe.apiml.gateway.websocket.WebSocketProperties;
import org.zowe.apiml.gateway.websocket.WebSocketHandshakePolicy;
import org.zowe.apiml.gateway.websocket.WebSocketRelayResources;
import org.zowe.apiml.gateway.websocket.ApimlWebSocketRoutingFilter;
import org.zowe.apiml.product.web.HttpConfig;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(WebSocketProperties.class)
public class WebSocketConfig {

    private final HttpClientProperties httpClientProperties;

    @Bean
    @Primary
    WebSocketClient webSocketClient(HttpConfig config, WebSocketProperties properties) {
        var maxFramePayloadLength = httpClientProperties.getWebsocket().getMaxFramePayloadLength();
        var webSocketContainer = new WsWebSocketContainer();
        webSocketContainer.setAsyncSendTimeout(properties.getAsyncWriteTimeout());
        webSocketContainer.setDefaultMaxSessionIdleTimeout(properties.getMaxIdleTimeout());
        if (maxFramePayloadLength != null) {
            webSocketContainer.setDefaultMaxBinaryMessageBufferSize(maxFramePayloadLength);
            webSocketContainer.setDefaultMaxTextMessageBufferSize(maxFramePayloadLength);
        }
        // Retain Tomcat: the previous Netty client could write before synchronous backends were ready (PR 4519).
        return new ApimlWebSocketClient(config.secureSslContext(), webSocketContainer, properties);
    }

    @Bean
    @Primary
    RequestUpgradeStrategy requestUpgradeStrategy(WebSocketProperties properties) {
        return new ApimlRequestUpgradeStrategy(httpClientProperties, properties);
    }

    @Bean
    WebSocketHandshakePolicy webSocketHandshakePolicy() {
        return new WebSocketHandshakePolicy();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "server.web-socket.partial", name = "enabled", havingValue = "true", matchIfMissing = true)
    WebSocketRelayResources webSocketRelayResources(WebSocketProperties properties, MeterRegistry meters) {
        return new WebSocketRelayResources(properties, meters);
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnProperty(prefix = "server.web-socket.partial", name = "enabled", havingValue = "true", matchIfMissing = true)
    WsWebSocketContainer partialWebSocketContainer(WebSocketProperties properties) {
        var container = new WsWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(properties.getPartial().getBinaryBufferSize());
        container.setDefaultMaxTextMessageBufferSize(properties.getPartial().getTextBufferSize());
        return container;
    }

    @Bean
    ApimlWebSocketRoutingFilter apimlWebSocketRoutingFilter(WebSocketProperties properties,
        ObjectProvider<WebSocketRelayResources> resources, WebSocketHandshakePolicy policy, ObjectProvider<WsWebSocketContainer> partialWebSocketContainer,
        HttpConfig config, WebSocketService service, ObjectProvider<List<HttpHeadersFilter>> headerFilters, WebSocketClient client) {
        return new ApimlWebSocketRoutingFilter(properties, httpClientProperties, resources, policy,
            partialWebSocketContainer, config, service, headerFilters, client);
    }

}
