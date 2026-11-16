/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import jakarta.websocket.*;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockWebSocketStreamingService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {"server.webSocket.partial.enabled=true",
    "apiml.security.ssl.nonStrictVerifySslCertificatesOfServices=true"})
class PartialWebSocketNonStrictTest extends AcceptanceTestWithMockServices {
    @TempDir
    Path directory;

    @Test
    void honorsTheConfiguredNonStrictTlsPolicy() throws Exception {
        var backend = new MockWebSocketStreamingService("streamingservice", directory, true, List.of(), service -> {
            applicationRegistry.update(service);
            updateRoutingRules();
        });
        backend.useTls(Path.of("../keystore/negative/hostname-mismatch.keystore.p12"),
            Path.of("../keystore/service/service.truststore.p12"), "password");
        backend.start();
        var container = new WsWebSocketContainer();
        Session client = null;
        try {
            client = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) { }
            }, ClientEndpointConfig.Builder.create().sslContext(apimlSSLContext).build(),
                URI.create("wss://localhost:" + port + "/streamingservice/ws/v1"));
            client.getBasicRemote().sendText("non-strict TLS", false);
            assertThat(backend.received.poll(10, TimeUnit.SECONDS))
                .isEqualTo(new MockWebSocketStreamingService.Received("non-strict TLS", false));
        } finally {
            if (client != null && client.isOpen()) client.close();
            container.destroy();
        }
    }
}
