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

import org.springframework.web.reactive.socket.WebSocketHandler;
import reactor.core.publisher.Mono;

/** Opens the backend before committing the client upgrade. An empty string means no subprotocol. */
public interface BackendWebSocketHandler extends WebSocketHandler {
    Mono<String> connect();

    void abort();
}
