/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.ws;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

public class HeaderSocketServerHandler extends AbstractWebSocketHandler {
    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage)
            throws Exception {
        String incomingMessage = webSocketMessage.getPayload().toString();
        HttpHeaders headers = webSocketSession.getHandshakeHeaders();

        webSocketSession.sendMessage(new TextMessage(headers.toString()));
        if (incomingMessage.equals("bye")) {
            webSocketSession.close();
        }
    }
}
