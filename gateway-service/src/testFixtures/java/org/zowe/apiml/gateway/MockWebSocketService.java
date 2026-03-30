/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway;

import com.netflix.appinfo.InstanceInfo.PortType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.SSLParametersWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;

@Slf4j
@Builder(builderClassName = "MockWsServiceBuilder", buildMethodName = "internalWsBuild", builderMethodName = "wsBuilder")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class MockWebSocketService extends MockService {

    private WebSocketServer webSocketServer;
    @Singular
    private List<Consumer<Object>> assertions;
    private SSLContext sslContext;

    @Override
    public void start() throws IOException {
        if (!status.get().isUp()) {
            this.init();
            webSocketServer.start();
            await()
                .atMost(Duration.ofSeconds(30))
                .until(() -> webSocketServer.getPort() != 0);

            this.port = webSocketServer.getPort();
            setStatus(Status.STARTED);
        }
    }

    private void init() {
        webSocketServer = new WebSocketServerImpl(new InetSocketAddress(getPort() > 1024 ? getPort() : 0));
        if (sslContext != null) {
            webSocketServer.setWebSocketFactory(new SSLParametersWebSocketServerFactory(sslContext, sslContext.getDefaultSSLParameters()));
        }

        if (getGatewayUrl() == null) gatewayUrl = "ws/v1";
        if (getServiceUrl() == null) serviceUrl = "/" + serviceId;
    }

    @Override
    public Map<? extends String, ? extends String> getAdditionalMetadata() {
        return Map.of(); //
    }

    @Override
    public void stop() {
        try {
            if (status.get().isUp()) {
                webSocketServer.stop();
            }
        } catch (InterruptedException e) {
            log.error("Failure stopping web socket server", e);
        }
        setStatus(Status.STOPPED);
    }

    @Override
    public void zombie() {
        if (status.get().isUp()) {
            try {
                webSocketServer.stop();
            } catch (InterruptedException e) {
                log.error("Failed to stop web socket server {}", e.getMessage());
            }
        }

        setStatus(Status.ZOMBIE);
    }

    @Override
    public com.netflix.appinfo.InstanceInfo.Builder getInstanceInfo() {
        var builder = super.getInstanceInfo();
        if (sslContext != null) {
            builder.setSecurePort(port);
            builder.enablePort(PortType.SECURE, true);
            builder.enablePort(PortType.UNSECURE, false);
        }
        return builder;
    }

    @Override
    Map<String, String> getMetadata() {
        var metadata = super.getMetadata();
        metadata.put("apiml.routes.ws-v1.gatewayUrl", "ws/v1");
        metadata.put("apiml.routes.ws-v1.serviceUrl", "/" + serviceId + "/ws");
        return metadata;
    }

    public static class MockWsServiceBuilder {

        private List<Consumer<MockService>> statusChangedlisteners = new ArrayList<>();
        private String serviceId;
        private String hostname;
        private int port;
        private SSLContext sslContext;
        private Scope scope;

        public MockWebSocketService build() {
            var mockWebSocketService = internalWsBuild();
            mockWebSocketService.hostname = StringUtils.isBlank(hostname) ? "localhost" : hostname;
            mockWebSocketService.statusChangedlisteners = this.statusChangedlisteners;
            mockWebSocketService.serviceId = this.serviceId;
            mockWebSocketService.port = port;
            mockWebSocketService.additionalMetadata = new HashMap<>();
            mockWebSocketService.sslContext = sslContext;
            mockWebSocketService.scope = scope;
            return mockWebSocketService;
        }

        public MockWebSocketService start() {
            var mockService = build();
            try {
                mockService.start();
            } catch (IOException e) {
                log.error("Failed starting web socket server {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
            return mockService;
        }

        public MockWsServiceBuilder statusChangedListener(Consumer<MockService> statusChangedListener) {
            this.statusChangedlisteners.add(statusChangedListener);
            return this;
        }

        public MockWsServiceBuilder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public MockWsServiceBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public MockWsServiceBuilder port(int port) {
            this.port = port;
            return this;
        }

        public MockWsServiceBuilder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public MockWsServiceBuilder scope(Scope scope) {
            this.scope = scope;
            return this;
        }

    }

    private class WebSocketServerImpl extends WebSocketServer {

        WebSocketServerImpl(InetSocketAddress address) {
            super(address);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            log.info("Opened WebSocket connection");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            log.info("Closed WebSocket connection");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            if (assertions != null) {
                assertions.forEach(assertion -> {
                    try {
                        assertion.accept(message);
                    } catch (AssertionError ae) {
                        setAssertionError(ae);
                    }
                });
            }
            conn.send("ACK:" + message);
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            if (assertions != null) {
                assertions.forEach(assertion -> {
                    try {
                        assertion.accept(message);
                    } catch (AssertionError ae) {
                        setAssertionError(ae);
                    }
                });
            }
            conn.send(message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            log.info("Error in WebSocket connection", ex);
        }

        @Override
        public void onStart() {
            log.info("Start WebSocket connection");
        }

    }

}
