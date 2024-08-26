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

/**
 * Default implementation. Provides the WebSocketRoutedSession the same way as before.
 */
public class WebSocketRoutedSessionFactoryImpl implements WebSocketRoutedSessionFactory {

    @Override
    public WebSocketRoutedSession session(
            WebSocketSession webSocketSession,
            String targetUrl,
            WebSocketClientFactory webSocketClientFactory,
            ClientSessionSuccessCallback successCallback,
            ClientSessionFailureCallback failureCallback) {
        return new WebSocketRoutedSession(webSocketSession, targetUrl, webSocketClientFactory, successCallback, failureCallback);
    }

}
