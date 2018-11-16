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
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Represents a connection in the proxying chain, establishes 'client' to
 * 'server' communication with the next server, with a
 * {@link WebSocketProxyClientHandler} to copy data from the 'client' to the
 * supplied 'server' session.
 */
@Slf4j
public class WebSocketRoutedSession {
    private static final int DEFAULT_TIMEOUT = 30000;

    private final WebSocketSession webSocketClientSession;
    private final WebSocketSession webSocketServerSession;
    private final SslContextFactory jettySslContextFactory;

    public WebSocketRoutedSession(WebSocketSession webSocketServerSession, String targetUrl, SslContextFactory jettySslContextFactory) {
        this.webSocketClientSession = createWebSocketClientSession(webSocketServerSession, targetUrl);
        this.webSocketServerSession = webSocketServerSession;
        this.jettySslContextFactory = jettySslContextFactory;
    }

    public WebSocketSession getWebSocketClientSession() {
        return webSocketClientSession;
    }

    public WebSocketSession getWebSocketServerSession() {
        return webSocketServerSession;
    }

    private WebSocketSession createWebSocketClientSession(WebSocketSession webSocketServerSession, String targetUrl) {
        try {
            log.debug("createWebSocketClientSession(session={},targetUrl={})", webSocketClientSession, targetUrl);
            JettyWebSocketClient client = new JettyWebSocketClient(new WebSocketClient(jettySslContextFactory));
            client.start();
            ListenableFuture<WebSocketSession> futureSession = client
                    .doHandshake(new WebSocketProxyClientHandler(webSocketServerSession), targetUrl);
            return futureSession.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new WebSocketProxyError(
                    String.format("Error opening session to WebSocket service at %s: %s", targetUrl, e.getMessage()), e,
                    webSocketServerSession);
        }
    }

    public void sendMessageToServer(WebSocketMessage<?> webSocketMessage) throws IOException {
        log.debug("sendMessageToServer(session={},message={})", webSocketClientSession, webSocketMessage);
        webSocketClientSession.sendMessage(webSocketMessage);
    }

    public void close(CloseStatus status) throws IOException {
        webSocketClientSession.close(status);
    }
}
