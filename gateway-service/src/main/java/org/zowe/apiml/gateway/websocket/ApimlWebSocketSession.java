/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import jakarta.websocket.Session;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.adapter.TomcatWebSocketSession;
import reactor.core.publisher.Sinks;

public class ApimlWebSocketSession extends TomcatWebSocketSession {

    private Sinks.Empty<Void> completionSink;

    public ApimlWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory, Sinks.Empty<Void> completionSink) {
        super(session, info, factory, completionSink);
        this.completionSink = completionSink;
    }

    public ApimlWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
        super(session, info, factory);
    }

    @Override
    public void onError(Throwable ex) {
        if (this.completionSink != null) {
            // Ignore result: can't overflow, ok if not first or no one listens
            this.completionSink.tryEmitError(ex);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket session completed with error", ex);
        } else if (logger.isInfoEnabled()) {
            logger.info("WebSocket session completed with error: " + ex.getMessage());
        }
        if (ex.getCause() instanceof AuthenticationException) {
            close(new CloseStatus(1003, "Invalid login credentials"));
        }
        close(CloseStatus.SERVER_ERROR);
    }
}
