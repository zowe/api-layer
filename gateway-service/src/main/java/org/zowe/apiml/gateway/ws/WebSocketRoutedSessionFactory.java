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

import org.springframework.web.socket.WebSocketSession;

public interface WebSocketRoutedSessionFactory {
    /**
     * Create valid client websocket session based on the existing session, target Url and SSL Context.
     * @param webSocketSession Valid Server side WebSocket Session.
     * @param targetUrl Full websocket URL towards the server
     * @param webSocketClientFactory Factory producing the current SSL Context.
     * @param successCallback callback to be called if the session is established successfully
     * @param failureCallback callback to be called if the session fails to be established
     */
    WebSocketRoutedSession session(
        WebSocketSession webSocketSession,
        String targetUrl,
        WebSocketClientFactory webSocketClientFactory,
        ClientSessionSuccessCallback successCallback,
        ClientSessionFailureCallback failureCallback);
}
