/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import javax.annotation.PreDestroy;

/**
 * Factory for provisioning web socket client
 * <p>
 * Manages the client lifecycle
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE) // for testing purposes
@Slf4j
public class WebSocketClientFactory {

    private final JettyWebSocketClient client;

    public WebSocketClientFactory(
            SslContextFactory.Client jettyClientSslContextFactory,
            @Value("${server.webSocket.maxIdleTimeout:3600000}") int maxIdleWebSocketTimeout,
            @Value("${server.webSocket.connectTimeout:15000}") long connectTimeout,
            @Value("${server.webSocket.stopTimeout:30000}") long stopTimeout,
            @Value("${server.webSocket.asyncWriteTimeout:60000}") long asyncWriteTimeout,
            @Value("${server.webSocket.requestBufferSize:8192}") int maxRequestBufferSize
        ) {
        log.debug("Creating Jetty WebSocket client, with SslFactory: {}", jettyClientSslContextFactory);

        HttpClient httpClient = new HttpClient(jettyClientSslContextFactory);
        httpClient.setRequestBufferSize(maxRequestBufferSize);
        WebSocketClient wsClient = new WebSocketClient(httpClient);

        wsClient.setMaxIdleTimeout(maxIdleWebSocketTimeout);
        wsClient.setConnectTimeout(connectTimeout);
        wsClient.setStopTimeout(stopTimeout);
        wsClient.setAsyncWriteTimeout(asyncWriteTimeout);
        client = new JettyWebSocketClient(wsClient);
        client.start();
    }

    JettyWebSocketClient getClientInstance() {
        return client;
    }

    @PreDestroy
    void closeClient() {
        if (client.isRunning()) {
            log.debug("Closing Jetty WebSocket client");
            client.stop();
        }

    }

}
