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

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import javax.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Represents a connection in the proxying chain, establishes 'client' to
 * 'server' communication with the next server, with a
 * {@link WebSocketProxyClientHandler} to copy data from the 'client' to the
 * supplied 'server' session.
 */
@Slf4j
public class WebSocketRoutedSession {

    private final ListenableFuture<WebSocketSession> webSocketClientSession;
    private final WebSocketSession webSocketServerSession;
    private final WebSocketProxyClientHandler clientHandler;
    private final String targetUrl;

    private WebSocketSession clientSession;

    public WebSocketRoutedSession(WebSocketSession webSocketServerSession, String targetUrl, WebSocketClientFactory webSocketClientFactory) {
        this.webSocketServerSession = webSocketServerSession;
        this.targetUrl = targetUrl;
        this.clientHandler = new WebSocketProxyClientHandler(webSocketServerSession);
        this.webSocketClientSession = createWebSocketClientSession(webSocketServerSession, targetUrl, webSocketClientFactory);
    }

    @VisibleForTesting
    WebSocketRoutedSession(WebSocketSession webSocketServerSession, ListenableFuture<WebSocketSession> webSocketClientSession, WebSocketProxyClientHandler clientHandler, String targetUrl) {
        this.webSocketClientSession = webSocketClientSession;
        this.webSocketServerSession = webSocketServerSession;
        this.clientHandler = clientHandler;
        this.targetUrl = targetUrl;
    }

    private WebSocketHttpHeaders getWebSocketHttpHeaders(WebSocketSession webSocketServerSession) {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        HttpHeaders browserHeaders = webSocketServerSession.getHandshakeHeaders();
        browserHeaders.forEach((key, value) -> {
            String header = String.join(" ", value);
            headers.add(key, header);
        });

        return headers;
    }

    public ListenableFuture<WebSocketSession> getWebSocketClientSession() {
        return webSocketClientSession;
    }

    public WebSocketSession getWebSocketServerSession() {
        return webSocketServerSession;
    }

    WebSocketProxyClientHandler getClientHandler() {
        return clientHandler;
    }

    void setClientSession(WebSocketSession clientSession) {
        this.clientSession = clientSession;
    }

    @Nullable
    WebSocketSession getClientSession() {
        return clientSession;
    }

    private ListenableFuture<WebSocketSession> createWebSocketClientSession(WebSocketSession webSocketServerSession, String targetUrl, WebSocketClientFactory webSocketClientFactory) {
        try {
            JettyWebSocketClient client = webSocketClientFactory.getClientInstance(targetUrl);
            URI targetURI = new URI(targetUrl);
            WebSocketHttpHeaders headers = getWebSocketHttpHeaders(webSocketServerSession);
            return client.doHandshake(clientHandler, headers, targetURI);
        } catch (IllegalStateException e) {
            throw webSocketProxyException(targetUrl, e, webSocketServerSession, true);
        } catch (Exception e) {
            throw webSocketProxyException(targetUrl, e, webSocketServerSession, false);
        }
    }

    private WebSocketProxyError webSocketProxyException(String targetUrl, Exception cause, WebSocketSession webSocketServerSession, boolean logError) {
        String message = String.format("Error opening session to WebSocket service at %s: %s", targetUrl, cause.getMessage());
        if (logError) {
            log.debug(message);
        }
        return new WebSocketProxyError(message, cause, webSocketServerSession);
    }

    public void sendMessageToServer(WebSocketMessage<?> webSocketMessage) throws IOException {
        log.debug("sendMessageToServer(session={}, message={})", webSocketClientSession, webSocketMessage);
        if (isClientConnected()) {
            try {
                clientSession.sendMessage(webSocketMessage);
            } catch (IOException e) {
                log.debug("Failed sending message: {}", webSocketMessage, e);
            }
        } else {
            log.debug("Tried to send a WebSocket message in a non-established client session.");
        }
    }

    public boolean isClientConnected() {
        return webSocketClientSession.isDone() && clientSession != null && clientSession.isOpen();
    }

    public void close(CloseStatus status) throws IOException {
        try {
            if (isClientConnected()) {
                clientSession.close(status);
            }
        } catch (IOException e) {
            log.debug("Failed to close WebSocket client session with status {}", status, e);
        }
    }

    public String getServerRemoteAddress() {
        InetSocketAddress address = getWebSocketServerSession().getRemoteAddress();
        if (address != null) {
            return address.toString();
        }

        return null;
    }

    public String getServerUri() {
        URI uri = getWebSocketServerSession().getUri();
        if (uri != null) {
            return uri.toString();
        }

        return null;
    }

    public String getClientUri() {
        return targetUrl;
    }

    /**
     * Get the sessionId for this session's client session
     *
     * @return WebSocket client session id or null if not established (could be temporary)
     *
     * @throws ServerNotYetAvailableException If a client session is not established
     */
    public String getClientId() {
        if (!webSocketClientSession.isDone()) {
            throw new ServerNotYetAvailableException();
        }
        if (isClientConnected()) {
            return clientSession.getId();
        }

        return null;
    }

}
