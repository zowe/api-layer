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

import com.sun.net.httpserver.Headers;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockWebSocketStreamingService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@MicroservicesAcceptanceTest
@TestPropertySource(properties = {
    "server.web-socket.partial.enabled=true",
    "server.web-socket.partial.maxConnections=1"
})
class PartialWebSocketHandshakeTest extends AcceptanceTestWithMockServices {
    @Autowired
    MeterRegistry meters;
    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({"true,401,1003", "false,401,1003", "true,403,1011", "false,403,1011", "true,404,1011", "false,404,1011"})
    void backendRejectionPreservesTheClientCloseReason(boolean partial, int status, int closeCode) throws Exception {
        var headers = new Headers();
        headers.add("WWW-Authenticate", "Basic realm=\"backend\"");
        var backend = mockService("rejectedservice")
            .additionalMetadata(Map.of(
                "apiml.routes.ws-v1.gatewayUrl", "ws/v1",
                "apiml.routes.ws-v1.serviceUrl", "/backend",
                "apiml.websocket.partialForwarding", Boolean.toString(partial)))
            .addEndpoint("/backend").responseCode(status).headers(headers).and().start();
        var closed = new LinkedBlockingQueue<CloseReason>();
        var container = new WsWebSocketContainer();
        Session client = null;
        try {
            client = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) { }

                @Override
                public void onClose(Session session, CloseReason reason) {
                    closed.add(reason);
                }
            }, ClientEndpointConfig.Builder.create().sslContext(apimlSSLContext).build(),
                URI.create("wss://localhost:" + port + "/rejectedservice/ws/v1"));
            var reason = closed.poll(10, TimeUnit.SECONDS);
            assertThat(reason).isNotNull();
            assertThat(reason.getCloseCode().getCode()).isEqualTo(closeCode);
            if (status == 401) {
                assertThat(reason.getReasonPhrase()).isEqualTo("Invalid login credentials");
            } else {
                assertThat(reason.getReasonPhrase()).contains("[" + status + "]");
            }
            assertThat(backend.getCounter()).isEqualTo(1);
        } finally {
            if (client != null && client.isOpen()) client.close();
            container.destroy();
        }
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            assertThat(meters.get("apiml.websocket.active").gauge().value()).isZero());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectedNativeUpgradeClosesTheAlreadyConnectedBackend(boolean partial) throws Exception {
        var backend = new MockWebSocketStreamingService("streamingservice", directory, partial, List.of("second"), service -> {
            applicationRegistry.update(service);
            updateRoutingRules();
        });
        backend.start();
        given().config(apimlCert.httpClient(httpClientConfig().setParam("http.socket.timeout", 5000)))
            .header("Upgrade", "websocket")
            .header("Connection", "Upgrade")
            .header("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
            .header("Sec-WebSocket-Version", "13")
            .header("Sec-WebSocket-Protocol", "first, second")
            .header("Sec-WebSocket-Extensions", "permessage-deflate; x=a=b")
            .get("https://localhost:" + port + "/streamingservice/ws/v1")
            .then().statusCode(400);
        assertThat(backend.connections).hasValue(1);
        assertThat(backend.closed.poll(10, TimeUnit.SECONDS)).isNotNull();
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            assertThat(meters.get("apiml.websocket.active").gauge().value()).isZero());
    }

    @ParameterizedTest
    @CsvSource({"12,dGhlIHNhbXBsZSBub25jZQ==,426", "13,invalid-key,400"})
    void rejectedUpgradeReleasesCapacityForAValidConnection(String version, String key, int status) throws Exception {
        var backend = new MockWebSocketStreamingService("streamingservice", directory, true, List.of(), service -> {
            applicationRegistry.update(service);
            updateRoutingRules();
        });
        backend.start();
        var uri = URI.create("wss://localhost:" + port + "/streamingservice/ws/v1");
        given().config(apimlCert.httpClient(httpClientConfig().setParam("http.socket.timeout", 5000)))
            .header("Upgrade", "websocket")
            .header("Connection", "Upgrade")
            .header("Sec-WebSocket-Key", key)
            .header("Sec-WebSocket-Version", version)
            .get("https://localhost:" + port + "/streamingservice/ws/v1")
            .then().statusCode(status);
        assertThat(backend.connections).hasValue(0);
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            assertThat(meters.get("apiml.websocket.active").gauge().value()).isZero());

        var container = new WsWebSocketContainer();
        Session client = null;
        try {
            client = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig config) { }
            }, ClientEndpointConfig.Builder.create().sslContext(apimlSSLContext).build(), uri);
            client.getBasicRemote().sendText("after rejected upgrade", false);
            assertThat(backend.received.poll(10, TimeUnit.SECONDS))
                .isEqualTo(new MockWebSocketStreamingService.Received("after rejected upgrade", false));
            assertThat(backend.connections).hasValue(1);
        } finally {
            if (client != null && client.isOpen()) client.close();
            container.destroy();
        }
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            assertThat(meters.get("apiml.websocket.active").gauge().value()).isZero());
    }
}
