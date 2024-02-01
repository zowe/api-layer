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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutionException;

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

    public WebSocketRoutedSession(WebSocketSession webSocketServerSession, String targetUrl, WebSocketClientFactory webSocketClientFactory) {
        this.webSocketServerSession = webSocketServerSession;
        this.webSocketClientSession = createWebSocketClientSession(webSocketServerSession, targetUrl, webSocketClientFactory);
    }

    public WebSocketRoutedSession(WebSocketSession webSocketServerSession, WebSocketSession webSocketClientSession) {
        this.webSocketClientSession = webSocketClientSession;
        this.webSocketServerSession = webSocketServerSession;
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

    public WebSocketSession getWebSocketClientSession() {
        return webSocketClientSession;
    }

    public WebSocketSession getWebSocketServerSession() {
        return webSocketServerSession;
    }

    private WebSocketSession createWebSocketClientSession(WebSocketSession webSocketServerSession, String targetUrl, WebSocketClientFactory webSocketClientFactory) {
        try {
            return null;
            //FIXME:
//            JettyWebSocketClient client = webSocketClientFactory.getClientInstance();
//            URI targetURI = new URI(targetUrl);
//            WebSocketHttpHeaders headers = getWebSocketHttpHeaders(webSocketServerSession);
//            ListenableFuture<WebSocketSession> futureSession = client
//                .doHandshake(new WebSocketProxyClientHandler(webSocketServerSession), headers, targetURI);
//            return futureSession.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (IllegalStateException e) {
            throw webSocketProxyException(targetUrl, e, webSocketServerSession, true);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw webSocketProxyException(targetUrl, e, webSocketServerSession, false);
//        } catch (ExecutionException e) {
//            throw handleExecutionException(targetUrl, e, webSocketServerSession, false);
        } catch (Exception e) {
            throw webSocketProxyException(targetUrl, e, webSocketServerSession, false);
        }
    }

    private WebSocketProxyError handleExecutionException(String targetUrl, ExecutionException cause, WebSocketSession webSocketServerSession, boolean logError) {
        if (cause.getCause() != null && cause.getCause().getCause() instanceof UpgradeException) {
            UpgradeException upgradeException = (UpgradeException) cause.getCause().getCause();
            if (upgradeException.getResponseStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
                String message = "Invalid login credentials";
                if (logError) {
                    log.debug(message);
                }
                return new WebSocketProxyError(message, cause, webSocketServerSession);
            } else {
                return webSocketProxyException(targetUrl, cause, webSocketServerSession, logError);
            }
        } else {
            return webSocketProxyException(targetUrl, cause, webSocketServerSession, logError);
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
        log.debug("sendMessageToServer(session={},message={})", webSocketClientSession, webSocketMessage);
        webSocketClientSession.sendMessage(webSocketMessage);
    }

    public void close(CloseStatus status) throws IOException {
        if (webSocketClientSession.isOpen()) {
            webSocketClientSession.close(status);
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
        URI uri = getWebSocketClientSession().getUri();
        if (uri != null) {
            return uri.toString();
        }

        return null;
    }

    public String getClientId() {
        return getWebSocketClientSession().getId();
    }
}
