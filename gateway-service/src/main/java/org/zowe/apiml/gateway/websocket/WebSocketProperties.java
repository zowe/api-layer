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

import jakarta.annotation.PostConstruct;
import jakarta.websocket.Session;
import lombok.Data;
import org.apache.tomcat.websocket.Constants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@Data
@ConfigurationProperties("server.web-socket")
public class WebSocketProperties {
    private long asyncWriteTimeout = 60_000;
    private long connectTimeout = 45_000;
    private long maxIdleTimeout = 3_600_000;
    private Partial partial = new Partial();

    @Data
    public static class Partial {
        private boolean enabled = true;
        private int binaryBufferSize = 65_536;
        private int textBufferSize = 32_768;
        private long pendingBytes = 1_048_576;
        private int pendingParts = 64;
        private long globalPendingBytes = 67_108_864;
        private int maxConnections = 256;
        private int writerThreads = 16;
        private int connectThreads = 4;
        private long maxBinaryMessageSize;
        private long maxTextMessageSize;
        private long messageTimeout = 120_000;
    }

    @PostConstruct
    public void validate() {
        Assert.isTrue(asyncWriteTimeout > 0 && connectTimeout > 0 && maxIdleTimeout >= 0,
            "WebSocket write/connect timeouts must be positive; idle timeout must not be negative");
        Assert.isTrue(partial.binaryBufferSize > 0 && partial.textBufferSize > 0, "WebSocket staging sizes must be positive");
        // A text part retains UTF-16 and may need a UTF-8 conversion during the send.
        long largestPart = Math.max(partial.binaryBufferSize, 5L * partial.textBufferSize) + 64;
        Assert.isTrue(partial.pendingBytes >= 2 * largestPart && partial.pendingParts >= 2,
            "WebSocket pending budget must accommodate two maximum-size parts");
        Assert.isTrue(partial.pendingBytes <= partial.globalPendingBytes / 2,
            "WebSocket global pending budget must accommodate both directions");
        Assert.isTrue(partial.maxConnections > 0 && partial.maxConnections <= 100_000
            && partial.writerThreads > 0 && partial.connectThreads > 0, "Invalid WebSocket worker/connection limits");
        Assert.isTrue(partial.maxBinaryMessageSize >= 0 && partial.maxTextMessageSize >= 0 && partial.messageTimeout > 0,
            "WebSocket message limits must not be negative and message timeout must be positive");
    }

    public void configure(Session session) {
        session.setMaxIdleTimeout(maxIdleTimeout);
        session.getAsyncRemote().setSendTimeout(asyncWriteTimeout);
        session.getUserProperties().put(Constants.BLOCKING_SEND_TIMEOUT_PROPERTY, asyncWriteTimeout);
    }
}
