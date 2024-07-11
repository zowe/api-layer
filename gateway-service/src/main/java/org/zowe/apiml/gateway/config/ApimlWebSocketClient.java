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

import jakarta.websocket.Session;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.StandardWebSocketSession;
import org.springframework.web.reactive.socket.client.StandardWebSocketClient;
import reactor.core.publisher.Sinks;

public class ApimlWebSocketClient extends StandardWebSocketClient {

    public ApimlWebSocketClient() {
        super(new WsWebSocketContainer());
    }

    @Override
    protected StandardWebSocketSession createWebSocketSession(
        Session session, HandshakeInfo info, Sinks.Empty<Void> completionSink) {

        return new ApimlWebSocketSession(session, info, bufferFactory(), completionSink);
    }
}
