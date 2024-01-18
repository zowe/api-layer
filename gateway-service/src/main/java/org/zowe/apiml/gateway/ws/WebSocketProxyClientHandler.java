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
import org.apache.http.HttpStatus;
import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.concurrent.TimeoutException;

/**
 * Copies data from the client to the server session.
 */
@Slf4j
public class WebSocketProxyClientHandler extends AbstractWebSocketHandler {
    private final WebSocketSession webSocketServerSession;

    public WebSocketProxyClientHandler(WebSocketSession webSocketServerSession) {
        this.webSocketServerSession = webSocketServerSession;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
        log.debug("handleMessage(session={},message={})", session, webSocketMessage);
        webSocketServerSession.sendMessage(webSocketMessage);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("afterConnectionClosed(session={},status={})", session, status);
        webSocketServerSession.close(status);
    }

    static CloseStatus getCloseStatusByResponseStatus(int responseStatus, String defaultMessage) {
        if (responseStatus >= 1000) {
            return new CloseStatus(responseStatus, defaultMessage);
        }

        if (responseStatus >= 500) {
            return CloseStatus.SERVER_ERROR.withReason(defaultMessage);
        }

        switch (responseStatus) {
            case HttpStatus.SC_UNAUTHORIZED:
                return CloseStatus.NOT_ACCEPTABLE.withReason("Invalid login credentials");
            default:
                return CloseStatus.NOT_ACCEPTABLE.withReason(defaultMessage);
        }
    }

    static CloseStatus getCloseStatusByError(Throwable exception) {
        if (exception instanceof CloseException) {
            CloseException closeException = (CloseException) exception;
            if (closeException.getCause() instanceof TimeoutException) {
                return CloseStatus.NORMAL;
            }
            return getCloseStatusByResponseStatus(closeException.getStatusCode(), String.valueOf(exception.getCause()));
        }

        if (exception instanceof UpgradeException) {
            UpgradeException upgradeException = (UpgradeException) exception;
            return getCloseStatusByResponseStatus(upgradeException.getResponseStatusCode(), String.valueOf(exception));
        }

        return CloseStatus.SERVER_ERROR.withReason(String.valueOf(exception));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("WebSocket transport error in session {}: {}", session.getId(), exception.getMessage());

        if (webSocketServerSession.isOpen()) {
            webSocketServerSession.close(getCloseStatusByError(exception));
        }

        super.handleTransportError(session, exception);
    }
}
