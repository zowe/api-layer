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

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@Builder(builderClassName = "MockWsServiceBuilder", buildMethodName = "internalWsBuild", builderMethodName = "wsBuilder")
@Getter
public class MockWebSocketService extends MockService {

    private WebSocketServer webSocketServer;
    @Singular
    private List<Consumer<String>> assertions;

    @Override
    public void start() throws IOException {
        if (!status.get().isUp()) {
            this.init();
            webSocketServer.start();
            port = webSocketServer.getPort();
            setStatus(Status.STARTED);
        }
    }

    private void init() {
        webSocketServer = new WebSocketServerImpl();

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
            setStatus(Status.STOPPED);
        } catch (InterruptedException e) {
            log.error("Failure stopping web socket server", e);
            setStatus(Status.ERROR);
        }
    }

    @Override
    public void zombie() {
        if (status.get().isUp()) {
            try {
                webSocketServer.stop();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                setStatus(Status.ERROR);
            }
        }

        setStatus(Status.ZOMBIE);
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

        public MockWebSocketService build() {
            var mockWebSocketService = internalWsBuild();
            mockWebSocketService.hostname = "localhost";
            mockWebSocketService.port = idCounter++;
            mockWebSocketService.statusChangedlisteners = this.statusChangedlisteners;
            mockWebSocketService.serviceId = this.serviceId;
            mockWebSocketService.additionalMetadata = new HashMap<>();
            return mockWebSocketService;
        }

        public MockWebSocketService start() {
            var mockService = build();
            try {
                mockService.start();
            } catch (RuntimeException | IOException e) {
                int i = atCounter.getAndIncrement();
                log.info("Not able to start mock server. Number of retries: {}", i);
                if (i < 4) {
                    start();
                }
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

        AtomicInteger atCounter = new AtomicInteger(0);

    }

    private class WebSocketServerImpl extends WebSocketServer {

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
            conn.send("ACK");
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
