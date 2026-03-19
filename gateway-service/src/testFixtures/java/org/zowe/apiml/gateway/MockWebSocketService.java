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

import com.sun.net.httpserver.HttpServer;
import groovy.transform.builder.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Builder(builderClassName = "MockWsServiceBuilder", buildMethodName = "internalBuild")
@Getter
public class MockWebSocketService extends MockService {

    private WebSocketServer webSocketServer;

    MockWebSocketService(int port, HttpServer server, List<Endpoint> endpointsConfig, String serviceId,
            String vipAddress, String hostname, String gatewayUrl, String serviceUrl,
            AuthenticationScheme authenticationScheme, String applid, Scope scope,
            List<Consumer<MockService>> statusChangedlisteners,
            Map<? extends String, ? extends String> additionalMetadata) {
        super(port, server, endpointsConfig, serviceId, vipAddress, hostname, gatewayUrl, serviceUrl, authenticationScheme,
                applid, scope, statusChangedlisteners, additionalMetadata);
    }

    @Override
    public void start() throws IOException {
        if (!status.get().isUp()) {
            this.init();
            webSocketServer.start();
            port = webSocketServer.getPort();
        }
        super.start();
    }

    private void init() {
        webSocketServer = new WebSocketServerImpl();
        endpoints.clear();
        endpointsConfig.forEach(endpoint -> {

        });
    }

    @Override
    public Map<? extends String, ? extends String> getAdditionalMetadata() {
        return Map.of(); //
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        super.close();
    }

    @Override
    public void stop() {
        try {
            webSocketServer.stop();
        } catch (InterruptedException e) {
            log.error("Failure stopping web socket server", e);
        }
    }

    public static class MockWsServiceBuilder {

        public MockWebSocketService build() {
            internalBuild();
            // var mockService = internalBuild();

            // return mockService;
            return null;
        }

        public MockWebSocketService start() {
            var mockService = build();
            try {
                mockService.start();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return mockService;
        }

    }

    private static class WebSocketServerImpl extends WebSocketServer {

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'onOpen'");
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'onClose'");
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'onMessage'");
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'onError'");
        }

        @Override
        public void onStart() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'onStart'");
        }

    }

}
