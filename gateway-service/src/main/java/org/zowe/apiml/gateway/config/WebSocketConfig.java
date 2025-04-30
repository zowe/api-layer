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

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.socket.client.JettyWebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.zowe.apiml.gateway.websocket.ApimlRequestUpgradeStrategy;

@Slf4j
@Configuration
public class WebSocketConfig {

    @Value("${server.webSocket.requestBufferSize:8192}")
    private int bufferSize;
    @Value("${server.webSocket.stopTimeout:30000}")
    private long stopTimeout;
    @Value("${server.webSocket.maxIdleTimeout:3600000}")
    private long idleTimeout;
    @Value("${server.webSocket.connectTimeout:45000}")
    private long connectTimeout;
    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;
    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    private WebSocketClient wsClient;
    private HttpClient httpClient;

    @Bean
    @Primary
    public JettyWebSocketClient webSocketClient(
        @Qualifier("jettyClientSslContextFactory")  SslContextFactory.Client sslContextFactory
    ) {
        try {
            if (verifySslCertificatesOfServices && nonStrictVerifySslCertificatesOfServices) {
                sslContextFactory.setEndpointIdentificationAlgorithm(null);
            }
            httpClient = new HttpClient();
            httpClient.setSslContextFactory(sslContextFactory);
            httpClient.setRequestBufferSize(bufferSize);
            httpClient.setConnectTimeout(connectTimeout);
            httpClient.setIdleTimeout(idleTimeout);
            httpClient.start();

            wsClient = new WebSocketClient(httpClient);
            wsClient.setConnectTimeout(connectTimeout);
            wsClient.setStopTimeout(stopTimeout);
            wsClient.start();

            log.info("Jetty WebSocketClient initialized successfully.");
            return new JettyWebSocketClient(wsClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Jetty WebSocketClient", e);
        }
    }

    @PreDestroy
    public void shutdownJettyClients() {
        try {
            if (wsClient != null && wsClient.isRunning()) {
                log.info("Stopping Jetty WebSocketClient...");
                wsClient.stop();
            }
            if (httpClient != null && httpClient.isRunning()) {
                log.info("Stopping Jetty HttpClient...");
                httpClient.stop();
            }
        } catch (Exception e) {
            log.warn("Error while shutting down Jetty clients", e);
        }
    }

    @Bean
    @Primary
    RequestUpgradeStrategy requestUpgradeStrategy() {
        return new ApimlRequestUpgradeStrategy();
    }

}
