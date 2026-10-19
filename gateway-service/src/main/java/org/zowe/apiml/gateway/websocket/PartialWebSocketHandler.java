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

import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import java.util.List;

/** Carries the native endpoint through Spring's normal HTTP handshake validation. */
public record PartialWebSocketHandler(PartialWebSocketRelay relay, List<String> protocols) implements BackendWebSocketHandler {
    @Override
    public Mono<String> connect() {
        return relay.connectBackend();
    }

    @Override
    public void abort() {
        relay.fail("handshake", "WebSocket handshake failed");
    }

    @Override
    public List<String> getSubProtocols() {
        return protocols;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        return Mono.error(new IllegalStateException("Partial forwarding requires the API ML upgrade strategy"));
    }
}
