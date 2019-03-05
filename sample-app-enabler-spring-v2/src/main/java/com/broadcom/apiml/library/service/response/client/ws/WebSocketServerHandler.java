/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.client.ws;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class WebSocketServerHandler extends AbstractWebSocketHandler {
    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage)
        throws Exception {
        String upperCaseText = webSocketMessage.getPayload().toString().toUpperCase();
        webSocketSession.sendMessage(new TextMessage(upperCaseText));
        if (upperCaseText.equals("BYE")) {
            webSocketSession.close();
        }
    }
}
