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

import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.util.Objects;

public class WebSocketHandshakePolicy {
    public static final String MISMATCH = "WebSocket subprotocol mismatch";

    public record Expected(String serviceId, String protocol) { }

    @InjectApimlLogger
    private final ApimlLogger log = ApimlLogger.empty();

    public boolean rejects(Expected expected, String actual) {
        String client = Objects.requireNonNullElse(expected.protocol(), "");
        String backend = Objects.requireNonNullElse(actual, "");
        if (client.equals(backend)) {
            return false;
        }
        log.log("org.zowe.apiml.gateway.webSocketSubprotocolMismatch", expected.serviceId(), client, backend);
        return true;
    }
}
