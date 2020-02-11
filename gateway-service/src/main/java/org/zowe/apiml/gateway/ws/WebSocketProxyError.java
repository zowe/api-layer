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

public class WebSocketProxyError extends RuntimeException {
    private static final long serialVersionUID = 6522624579669891882L;

    private final transient WebSocketSession session;

    public WebSocketProxyError(String message, Throwable cause, WebSocketSession session) {
        super(message, cause);
        this.session = session;
    }

    public WebSocketSession getSession() {
        return this.session;
    }
}
