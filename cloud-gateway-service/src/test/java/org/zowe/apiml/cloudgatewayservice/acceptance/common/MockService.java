/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.netflix.appinfo.InstanceInfo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import org.apache.http.HttpHeaders;
import org.assertj.core.error.MultipleAssertionsError;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.http.MediaType;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Builder(builderClassName = "MockServiceBuilder")
@Getter
public class MockService implements AutoCloseable {

    private HttpServer server;

    private String serviceId;

    private String gatewayUrl;
    private String serviceUrl;

    private AuthenticationScheme authenticationScheme;
    private String applid;

    @Singular
    private List<Consumer<MockService>> statusChangedlisteners;

    private final Map<String, Endpoint> endpoints = new HashMap<>();
    private final AtomicBoolean status = new AtomicBoolean(false);

    private static AssertionError assertionError;

    private void init(List<Endpoint> endpoints) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        endpoints.forEach(endpoint -> {
            if (this.endpoints.put(endpoint.getPath(), endpoint) != null) {
                throw new IllegalStateException("Duplicity of endpoints: " + endpoint.getPath());
            }

            server.createContext(endpoint.getPath(), endpoint::process);
        });

        if (gatewayUrl == null) gatewayUrl = "api/v1";
        if (serviceUrl == null) serviceUrl = "/" + serviceId;

        server.setExecutor(null);
    }

    private void fireStatusChanged() {
        if (statusChangedlisteners != null) {
            statusChangedlisteners.forEach(l -> l.accept(MockService.this));
        }
    }

    private static void setAssertionError(AssertionError assertionError) {
        if (MockService.assertionError == null) {
            MockService.assertionError = assertionError;
        } else {
            List<AssertionError> allErrors = new LinkedList<>();
            if (MockService.assertionError instanceof MultipleAssertionsError) {
                allErrors.addAll(((MultipleAssertionsError) MockService.assertionError).getErrors());
            }
            allErrors.add(assertionError);
            MockService.assertionError = new MultipleAssertionsError(allErrors);
        }
    }

    public static void checkAssertionErrors() {
        AssertionError assertionError = MockService.assertionError;
        if (assertionError != null) {
            MockService.assertionError = null;
            throw assertionError;
        }
    }

    public void start() {
        server.start();
        status.set(true);
        fireStatusChanged();
    }

    public void stop() {
        status.set(false);
        server.stop(0);
        fireStatusChanged();
    }

    public boolean isStarted() {
        return status.get();
    }

    @Override
    public void close() {
        statusChangedlisteners = null;
        if (status.get()) stop();
    }

    private Map<String, String> getMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.routes.api-v1.gatewayUrl", "api/v1");
        metadata.put("apiml.routes.api-v1.serviceUrl", "/" + serviceId);

        if (authenticationScheme != null) {
            metadata.put("apiml.authentication.scheme", authenticationScheme.getScheme());
        }
        if (applid != null) {
            metadata.put("apiml.authentication.applid", applid);
        }

        return metadata;
    }

    public InstanceInfo getInstanceInfo() {
        if (!status.get()) return null;

        InetSocketAddress address = server.getAddress();
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setHostName("localhost"/*address == null ? null : address.getHostName()*/)
            .setPort(address == null ? null : address.getPort())
            .setAppName(serviceId)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(getMetadata())
            .build();
    }

    public EurekaServiceInstance getEurekaServiceInstance() {
        InstanceInfo instanceInfo = getInstanceInfo();
        return instanceInfo == null ? null : new EurekaServiceInstance(instanceInfo);
    }

    public static class MockServiceBuilder {

        private List<Endpoint> endpoints = new LinkedList<>();

        public Endpoint.EndpointBuilder addEndpoint(String path) {
            Endpoint.EndpointBuilder endpointBuilder = Endpoint.builder();
            endpointBuilder.path(path);
            endpointBuilder.mockServiceBuilder = this;
            return endpointBuilder;
        }

        public MockService start() throws IOException {
            MockService mockService = build();
            mockService.init(endpoints);
            mockService.start();
            return mockService;
        }

    }

    @Builder
    @Value
    public static class Endpoint {

        @Builder.Default
        private int responseCode = 200;
        private String contentType;
        private String body;
        @Builder.Default
        private String path = "/";

        @Singular
        private List<Consumer<HttpExchange>> assertions;

        @Builder.Default
        private AtomicInteger counter = new AtomicInteger();

        void process(HttpExchange httpExchange) throws IOException {
            try {
                if (contentType != null) {
                    httpExchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
                }

                httpExchange.sendResponseHeaders(responseCode, 0);

                if (body != null) {
                    httpExchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
                }

                if (assertions != null) {
                    assertions.forEach(a -> a.accept(httpExchange));
                }

                httpExchange.getResponseBody().close();
            } catch (AssertionError afe) {
                setAssertionError(afe);
                throw afe;
            } finally {
                counter.getAndIncrement();
            }
        }

        public int getCounter() {
            return counter.get();
        }

        public void resetCounter() {
            counter.set(0);
        }

        public static class EndpointBuilder {

            private MockServiceBuilder mockServiceBuilder;

            public MockServiceBuilder and() {
                Endpoint endpoint = build();
                mockServiceBuilder.endpoints.add(endpoint);
                return mockServiceBuilder;
            }

            public EndpointBuilder bodyJson(Object body) throws JsonProcessingException {
                ObjectWriter writer = new ObjectMapper().writer();
                contentType(MediaType.APPLICATION_JSON_VALUE);
                return body(writer.writeValueAsString(body));
            }

        }

    }

}
