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

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * WebSocket diagnostic endpoint that reports the handshake headers back to the caller.
 */
public class HeaderSocketServerHandler extends AbstractWebSocketHandler {

    /**
     * Renders the handshake headers as {@code [name:"value", name:"value"]}.
     *
     * <p>This is the endpoint's own serialization contract, deliberately not
     * {@link HttpHeaders#toString()}: that is a framework debug representation whose shape and
     * casing are free to change between Spring versions, and the integration tests match on it.
     * The names are lower-cased for the same reason the HTTP diagnostic endpoint lower-cases them -
     * the container reports them as they arrived on the wire, so a Tomcat upgrade alone would
     * otherwise change the output.
     *
     * <p>Multiple values of the same header are joined with a comma. {@link Locale#ROOT} keeps the
     * result independent of the host locale.
     */
    static String describe(HttpHeaders headers) {
        return headers.headerSet().stream()
            .map(entry -> entry.getKey().toLowerCase(Locale.ROOT) + ":\"" + String.join(",", entry.getValue()) + "\"")
            .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage)
            throws Exception {
        String incomingMessage = webSocketMessage.getPayload().toString();

        webSocketSession.sendMessage(new TextMessage(describe(webSocketSession.getHandshakeHeaders())));
        if (incomingMessage.equals("bye")) {
            webSocketSession.close();
        }
    }
}
