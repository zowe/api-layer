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

import jakarta.websocket.*;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import com.netflix.appinfo.InstanceInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Real Tomcat partial endpoint. Tests control the two directions independently. */
public class MockWebSocketStreamingService extends MockService {
    public record Received(Object data, boolean last) { }
    public final BlockingQueue<Received> received = new LinkedBlockingQueue<>();
    public final BlockingQueue<Session> opened = new LinkedBlockingQueue<>();
    public final BlockingQueue<CloseReason> closed = new LinkedBlockingQueue<>();
    public final AtomicInteger connections = new AtomicInteger();
    public Consumer<Session> onOpen = session -> { };
    private final Tomcat tomcat = new Tomcat();
    private final List<String> protocols;
    private final Boolean partial;
    private boolean secure;

    public MockWebSocketStreamingService(String id, Path directory, Boolean partial, List<String> protocols, Consumer<MockService> listener) {
        this.serviceId = id;
        this.hostname = "localhost";
        this.additionalMetadata = Map.of();
        this.partial = partial;
        this.protocols = protocols;
        this.statusChangedlisteners = List.of(listener);
        this.scope = Scope.TEST;
        tomcat.setBaseDir(directory.toString());
        tomcat.setPort(0);
        tomcat.getConnector();
        var context = tomcat.addContext("", directory.toString());
        Tomcat.addServlet(context, "default", new org.apache.catalina.servlets.DefaultServlet());
        context.addServletMappingDecoded("/", "default");
        context.addServletContainerInitializer(new WsSci(), Set.of());
    }

    public void useTls(Path keyStore, Path trustStore, String password) {
        secure = true;
        var connector = tomcat.getConnector();
        connector.setSecure(true);
        connector.setScheme("https");
        connector.setProperty("SSLEnabled", "true");
        var ssl = new SSLHostConfig();
        ssl.setCertificateVerification("required");
        ssl.setTruststoreFile(trustStore.toAbsolutePath().toString());
        ssl.setTruststorePassword(password);
        ssl.setTruststoreType("PKCS12");
        var certificate = new SSLHostConfigCertificate(ssl, SSLHostConfigCertificate.Type.RSA);
        certificate.setCertificateKeystoreFile(keyStore.toAbsolutePath().toString());
        certificate.setCertificateKeystorePassword(password);
        certificate.setCertificateKeyPassword(password);
        certificate.setCertificateKeystoreType("PKCS12");
        ssl.addCertificate(certificate);
        connector.addSslHostConfig(ssl);
    }

    @Override
    public InstanceInfo.Builder getInstanceInfo() {
        return super.getInstanceInfo().setSecurePort(port)
            .enablePort(InstanceInfo.PortType.SECURE, secure)
            .enablePort(InstanceInfo.PortType.UNSECURE, !secure);
    }

    @Override
    public void start() throws IOException {
        try {
            tomcat.start();
            var context = tomcat.getHost().findChildren()[0];
            var servletContext = ((org.apache.catalina.Context) context).getServletContext();
            var container = (ServerContainer) servletContext.getAttribute(ServerContainer.class.getName());
            var config = ServerEndpointConfig.Builder.create(jakarta.websocket.Endpoint.class, "/stream").subprotocols(protocols)
                .configurator(new ServerEndpointConfig.Configurator() {
                    @Override
                    public <T> T getEndpointInstance(Class<T> type) {
                        return type.cast(new jakarta.websocket.Endpoint() {
                            @Override
                            public void onOpen(Session session, EndpointConfig configuration) {
                                connections.incrementAndGet();
                                session.setMaxBinaryMessageBufferSize(65_536);
                                session.setMaxTextMessageBufferSize(32_768);
                                session.addMessageHandler(ByteBuffer.class, (MessageHandler.Partial<ByteBuffer>) (data, last) -> {
                                    byte[] copy = new byte[data.remaining()];
                                    data.get(copy);
                                    received.add(new Received(copy, last));
                                });
                                session.addMessageHandler(String.class, (MessageHandler.Partial<String>) (data, last) -> received.add(new Received(data, last)));
                                opened.add(session);
                                onOpen.accept(session);
                            }

                            @Override
                            public void onClose(Session session, CloseReason reason) {
                                closed.add(reason);
                            }
                        });
                    }
                }).build();
            container.addEndpoint(config);
            port = tomcat.getConnector().getLocalPort();
            if (port <= 0) throw new IOException("WebSocket test connector did not start");
            setStatus(Status.STARTED);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    Map<String, String> getMetadata() {
        var metadata = super.getMetadata();
        metadata.put("apiml.routes.ws-v1.gatewayUrl", "ws/v1");
        metadata.put("apiml.routes.ws-v1.serviceUrl", "/stream");
        if (partial != null) metadata.put("apiml.websocket.partialForwarding", partial.toString());
        return metadata;
    }

    @Override
    public void stop() {
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            setStatus(Status.STOPPED);
        }
    }
}
