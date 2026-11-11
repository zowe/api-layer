/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import jakarta.websocket.*;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.gateway.MockWebSocketStreamingService;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@AcceptanceTest
class PartialWebSocketTest extends AcceptanceTestWithMockServices {
    @TempDir
    Path directory;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void routesWebSocketsThroughTheCombinedGateway(boolean partial) throws Exception {
        var backend = new MockWebSocketStreamingService("streamingservice", directory, partial ? null : Boolean.FALSE, List.of("test"), service -> {
            applicationRegistry.update(service);
            updateRoutingRules();
        });
        backend.start();
        var received = new LinkedBlockingQueue<MockWebSocketStreamingService.Received>();
        var container = new WsWebSocketContainer();
        Session client = null;
        try {
            client = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    session.addMessageHandler(String.class, (MessageHandler.Partial<String>)
                        (data, last) -> received.add(new MockWebSocketStreamingService.Received(data, last)));
                }
            }, ClientEndpointConfig.Builder.create().sslContext(httpConfig.secureSslContext())
                .preferredSubprotocols(List.of("unsupported", "test")).build(),
                URI.create("wss://localhost:" + port + "/streamingservice/ws/v1"));
            var service = backend.opened.poll(10, TimeUnit.SECONDS);
            assertThat(service).isNotNull();
            assertThat(client.getNegotiatedSubprotocol()).isEqualTo("test");
            assertThat(client.getNegotiatedSubprotocol()).isEqualTo(service.getNegotiatedSubprotocol());
            client.getBasicRemote().sendText("request", !partial);
            assertThat(backend.received.poll(10, TimeUnit.SECONDS))
                .isEqualTo(new MockWebSocketStreamingService.Received("request", !partial));
            service.getBasicRemote().sendText("response", !partial);
            assertThat(received.poll(10, TimeUnit.SECONDS))
                .isEqualTo(new MockWebSocketStreamingService.Received("response", !partial));
            if (partial) {
                client.getBasicRemote().sendText("", true);
                service.getBasicRemote().sendText("", true);
                assertThat(backend.received.poll(10, TimeUnit.SECONDS))
                    .isEqualTo(new MockWebSocketStreamingService.Received("", true));
                assertThat(received.poll(10, TimeUnit.SECONDS))
                    .isEqualTo(new MockWebSocketStreamingService.Received("", true));
            }
            assertThat(backend.connections).hasValue(1);
        } finally {
            if (client != null && client.isOpen()) client.close();
            container.destroy();
        }
    }
}
