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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.PortType;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.tyrus.server.Server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@Builder(builderClassName = "MockWsTyrusServiceBuilder", buildMethodName = "internalWsTyrusBuild", builderMethodName = "wsTyrusBuilder")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class MockWebSocketTyrusService extends MockWebSocketService {

    private Server tyrusServer;

    @Override
    public void start() throws IOException {
        if (!status.get().isUp()) {
            this.init();
            try {
                this.tyrusServer.start();
                port = tyrusServer.getPort();
            } catch (DeploymentException e) {
                throw new IOException(e);
            }
            setStatus(Status.STARTED);
        }
    }

    private void init() {
        this.tyrusServer = new Server(hostname, port, "/", null, BinaryEchoServer.class);

        if (getGatewayUrl() == null) gatewayUrl = "ws/v1";
        if (getServiceUrl() == null) serviceUrl = "/" + serviceId;
    }

    @Override
    public void stop() {
        if (status.get().isUp()) {
            tyrusServer.stop();
        }
        setStatus(Status.STOPPED);
    }

    @Override
    public void zombie() {
        if (status.get().isUp()) {
            tyrusServer.stop();
        }

        setStatus(Status.ZOMBIE);
    }

    @Override
    Map<String, String> getMetadata() {
        return super.getMetadata();
    }

    @Override
    public InstanceInfo.Builder getInstanceInfo() {
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(getInstanceId())
            .setHostName(hostname)
            .setPort(port)
            .enablePort(PortType.UNSECURE, true)
            .setAppName(serviceId)
            .setVIPAddress(vipAddress != null ? vipAddress : serviceId)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(getMetadata());
    }

    public static class MockWsTyrusServiceBuilder {

        private List<Consumer<MockService>> statusChangedlisteners = new ArrayList<>();
        private String serviceId;
        private String hostname;
        private int port;

        public MockWebSocketTyrusService build() {
            var mockWebSocketService = internalWsTyrusBuild();
            mockWebSocketService.statusChangedlisteners = this.statusChangedlisteners;
            mockWebSocketService.hostname = StringUtils.isBlank(hostname) ? "localhost" : hostname;
            mockWebSocketService.serviceId = this.serviceId;
            mockWebSocketService.port = port;
            mockWebSocketService.additionalMetadata = new HashMap<>();
            return mockWebSocketService;
        }

        public MockWebSocketTyrusService start() {
            var mockService = build();
            try {
                mockService.start();
            } catch (IOException e) {
                int i = atCounter.getAndIncrement();
                log.info("Not able to start mock server. Number of retries: {}", i);
                if (i < 4) {
                    start();
                }
            }
            return mockService;
        }

        public MockWsTyrusServiceBuilder statusChangedListener(Consumer<MockService> statusChangedListener) {
            this.statusChangedlisteners.add(statusChangedListener);
            return this;
        }

        public MockWsTyrusServiceBuilder serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        public MockWsTyrusServiceBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public MockWsTyrusServiceBuilder port(int port) {
            this.port = port;
            return this;
        }

        AtomicInteger atCounter = new AtomicInteger(0);

    }

    @ServerEndpoint("/echo")
    @Slf4j
    public static class BinaryEchoServer {

        @OnMessage
        public void onBinaryMessage(ByteBuffer message, Session session) {
            log.info("Got binary message {} bytes", message.remaining());
            try {
                sendBinaryChunked(session, message, 8192);
            } catch (IOException e) {
                log.error("Error sending back binary chunk: {}", e.getMessage(), e);
            }
        }

        public static void sendBinaryChunked(Session session, ByteBuffer buffer, int chunkSize) throws IOException {
            ByteBuffer src = buffer.duplicate();
            while (src.hasRemaining()) {
                int len = Math.min(chunkSize, src.remaining());
                ByteBuffer chunk = src.slice();
                chunk.limit(len);
                src.position(src.position() + len);
                session.getBasicRemote().sendBinary(chunk, !src.hasRemaining());
            }

        }

    }

}
