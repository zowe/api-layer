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
import com.netflix.discovery.shared.Application;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.*;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class allows to mock any simply service to a functional test. It is fully integrated in
 * {@link AcceptanceTestWithMockServices}. In case you are using directly this implementation DO NOT FORGET to close
 * the service once it is released and in the similar way, without {@link AcceptanceTestWithMockServices} it is
 * necessary to mock registry and routing. The easiest way is to use the method
 * {@link AcceptanceTestWithMockServices#mockService(String)}. It allows to you to use the same features and also
 * takes care about clean up, mocking of service register, and updating routing rules.
 *
 * Example:
 *
 *  try (MockService mockservice = MockService.builder()
 *      .serviceId("myservice")
 *      .scope(MockService.Scope.CLASS)
 *      .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET).applid("MYAPPLID")
 *      .addEndpoint("/test")
 *          .responseCode(403)
 *          .bodyJson("{\"error\": \"authenticatin failed\"}")
 *          .assertions(httpExchange -> assertNull(he.getRequestHeaders().getFirst("X-My-Header")))
 *      .and().addEndpoint("/404")
 *          .responseCode(404)
 *      .and().start()
 *  ) {
 *      // do a test
 *
 *      assertEquals(5, mockservice.getCounter());
 *      MockService.checkAssertionErrors();
 *  }
 *
 * Note: Before implementation please check the full list of methods.
 */
@Builder(builderClassName = "MockServiceBuilder", buildMethodName = "internalBuild")
@Getter
public class MockService implements AutoCloseable {

    private static int idCounter = 1;
    // in case on zombie mode is necessary to have a unique port number, on start replaced with the real one
    private int port;

    /**
     * HTTP server to handle requests and the endpoint configuration
     */
    @Getter(AccessLevel.NONE)
    private HttpServer server;
    @Getter(AccessLevel.NONE)
    private List<Endpoint> endpointsConfig;

    /**
     * Service identification
     */
    private String serviceId;
    private String vipAddress;
    @Builder.Default
    private String hostname = "localhost";

    /**
     * Routing configuration
     */
    private String gatewayUrl;
    private String serviceUrl;

    /**
     * Authentication configuration
     */
    private AuthenticationScheme authenticationScheme;
    private String applid;

    /**
     * It defines till when should be service instance available - it should be handled by an external component, i.e.
     * {@link AcceptanceTestWithMockServices} use it to releasing an instance.
     */
    @Builder.Default
    private Scope scope = Scope.TEST;

    @Singular
    @Getter(AccessLevel.NONE)
    private List<Consumer<MockService>> statusChangedlisteners;

    /**
     * All registered endpoints. It is possible to get any instance by path. If there is just one endpoint in the
     * service, you can use {@link MockService#getEndpoint()}
     */
    private final Map<String, Endpoint> endpoints = new HashMap<>();

    /**
     * Status of the service - see possible values {@link MockService.Status}
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<Status> status = new AtomicReference<>(Status.STOPPED);

    /**
     * Collector of assert error on server side. To throw them in a test is necessary to call
     * method (see {@link MockService#checkAssertionErrors()})
     */
    private static AssertionError assertionError;

    private void init() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        endpoints.clear();
        endpointsConfig.forEach(endpoint -> {
            if (endpoints.put(endpoint.getPath(), endpoint) != null) {
                throw new IllegalStateException("Duplicity of endpoints: " + endpoint.getPath());
            }

            server.createContext(endpoint.getPath(), endpoint::process);
        });

        if (gatewayUrl == null) gatewayUrl = "api/v1";
        if (serviceUrl == null) serviceUrl = "/" + serviceId;

        server.setExecutor(null);
    }

    public Status getStatus() {
        return status.get();
    }

    public String getInstanceId() {
        return hostname + ":" + getServiceId() + ":" + port;
    }

    private void fireStatusChanged() {
        if (statusChangedlisteners != null) {
            statusChangedlisteners.forEach(l -> l.accept(MockService.this));
        }
    }

    private static void setAssertionError(AssertionError assertionError) {
        if (MockService.assertionError == null) {
            // in case of the first error, just store the exception
            MockService.assertionError = assertionError;
        } else {
            // there was another exception in the past, create multiple assertion error collection all the errors
            List<AssertionError> allErrors = new LinkedList<>();
            if (MockService.assertionError instanceof MultipleAssertionsError) {
                allErrors.addAll(((MultipleAssertionsError) MockService.assertionError).getErrors());
            }
            allErrors.add(assertionError);
            MockService.assertionError = new MultipleAssertionsError(allErrors);
        }
    }

    /**
     * To throw assertion errors. The method clean all stored assertion errors, it means after invoking the mock
     * service is ready to next testing.
     */
    public static void checkAssertionErrors() {
        AssertionError assertionError = MockService.assertionError;
        if (assertionError != null) {
            MockService.assertionError = null;
            throw assertionError;
        }
    }

    private void setStatus(Status status) {
        if (this.status.get() != status) {
            this.status.set(status);
            fireStatusChanged();
        }
    }

    /**
     * To start the service.
     */
    public void start() throws IOException {
        if (!status.get().isUp()) {
            init();
            server.start();
            port = server.getAddress().getPort();
        }
        setStatus(Status.STARTED);
    }

    /**
     * To stop the service. If you want release the whole service, consider calling {@link MockService#close()}
     */
    public void stop() {
        if (status.get().isUp()) {
            server.stop(0);
        }
        setStatus(Status.STOPPED);
    }

    /**
     * To stop service without any notification (to be still in the registry). In the case service is down, just notify
     * to be in the registry.
     */
    public void zombie() {
        if (status.get().isUp()) {
            server.stop(0);
        }

        setStatus(Status.ZOMBIE);
    }

    /**
     * The method returns the endpoint if there is just one registered, otherwise end with an exception.
     * @return once registred endpoint
     */
    public Endpoint getEndpoint() {
        assertEquals(1, endpoints.size(), "There are more than one endpoint, please use method getEndpoints and select one");
        return endpoints.values().stream().findFirst().get();
    }

    /**
     * @return the sum of all endpoints counters (of attempts / requests)
     */
    public int getCounter() {
        int out = 0;
        for (Endpoint endpoint : endpoints.values()) {
            out += endpoint.getCounter();
        }
        return out;
    }

    /**
     * It reset counters (of attempts / requests) in all endpoints
     */
    public void resetCounter() {
        endpoints.values().forEach(Endpoint::resetCounter);
    }

    /**
     * Remove all listeners of changing status. It could be helpful if the case of removing mock service to avoid
     * back calls.
     */
    public void cleanStatusChangedListeners() {
        statusChangedlisteners = null;
    }

    /**
     * Method to use on the end to stop service (if it is running) and release resource. This method avoid back calls
     * to listeners of change status (using {@MockService#cleanStatusChangedListeners()}).
     */
    @Override
    public void close() {
        cleanStatusChangedListeners();
        stop();
        status.set(Status.CANCELLING);
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

    /**
     * Construct InstanceInfo for the mock service
     * @return instanceInfo with all related data
     */
    public InstanceInfo getInstanceInfo() {
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setHostName(hostname)
            .setPort(port)
            .setAppName(serviceId)
            .setVIPAddress(vipAddress != null ? vipAddress : serviceId)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(getMetadata())
            .build();
    }

    /**
     * Method call {@link MockService#getInstanceInfo()} converted to EurekaServiceInstance
     * @return EurekaServiceInstance with all related data
     */
    public EurekaServiceInstance getEurekaServiceInstance() {
        InstanceInfo instanceInfo = getInstanceInfo();
        return instanceInfo == null ? null : new EurekaServiceInstance(instanceInfo);
    }

    /**
     * Construct Application object using InstanceInfo from {@link MockService#getInstanceInfo()}
     * @return Eureka Application object
     */
    public Application getApplication() {
        InstanceInfo instanceInfo = getInstanceInfo();
        Application application = new Application(instanceInfo.getId());
        application.addInstance(instanceInfo);
        return application;
    }

    public static class MockServiceBuilder {

        private List<Endpoint> endpoints = new LinkedList<>();

        /**
         * Create a new endpoint of the Mock Service
         * @param path Path of the endpoint
         * @return builder to define other values
         */
        public Endpoint.EndpointBuilder addEndpoint(String path) {
            Endpoint.EndpointBuilder endpointBuilder = Endpoint.builder();
            endpointBuilder.path(path);
            endpointBuilder.mockServiceBuilder = this;
            return endpointBuilder;
        }

        /**
         * To build mock service. It will be stopped (not registred). It is necessary to call method start or zombie.
         * @return instance of mockService
         */
        public MockService build() {
            MockService mockService = internalBuild();
            mockService.port = idCounter++;
            mockService.endpointsConfig = endpoints;
            return mockService;
        }

        /**
         * To start build and start MockService
         * @return instance of MockService
         * @throws IOException - in case of any issue with starting server
         */
        public MockService start() throws IOException {
            MockService mockService = build();
            mockService.start();
            return mockService;
        }

    }

    @Builder
    @Value
    public static class Endpoint {

        /**
         * Response code of a response, as default 200
         */
        @Builder.Default
        private int responseCode = 200;

        /**
         * Content type of the response. As default null (no header is generated).
         */
        private String contentType;

        /**
         * Response body to answer
         */
        private String body;

        /**
         * Path of the endpoint
         */
        private String path;

        /**
         * Lambdas about assertion on server side. The outcome exception could be thrown by
         * {@link MockService#checkAssertionErrors()}
         */
        @Singular
        private List<Consumer<HttpExchange>> assertions;

        /**
         * Counter of calls. It contains amount of received requests.
         */
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
                    assertions.forEach(a -> {
                        try {
                            a.accept(httpExchange);
                        } catch (AssertionError afe) {
                            setAssertionError(afe);
                        }
                    });
                }

                httpExchange.getResponseBody().close();
            } finally {
                counter.getAndIncrement();
            }
        }

        /**
         * @return count of received requests since service is available or the last call of {@link Endpoint#resetCounter()}
         */
        public int getCounter() {
            return counter.get();
        }

        /**
         * To reset counter of received requests
         */
        public void resetCounter() {
            counter.set(0);
        }

        public static class EndpointBuilder {

            private MockServiceBuilder mockServiceBuilder;

            /**
             * Definition of the endpoint is done, continue with defining of the MockService
             * @return instance of MockService's builder
             */
            public MockServiceBuilder and() {
                Endpoint endpoint = build();
                mockServiceBuilder.endpoints.add(endpoint);
                return mockServiceBuilder;
            }

            /**
             * To set body and content type to application/json
             * @param body object to be converted to the json (to be returned in the response)
             * @return builder of the endpoint
             * @throws JsonProcessingException in case an issue with generation of JSON
             */
            public EndpointBuilder bodyJson(Object body) throws JsonProcessingException {
                ObjectWriter writer = new ObjectMapper().writer();
                contentType(MediaType.APPLICATION_JSON_VALUE);
                return body(writer.writeValueAsString(body));
            }

        }

    }

    public enum Scope {

        // the service should be stopped once the test (method) is done
        TEST,
        // the service should be stopped after evaluating all tests (methods) in the class
        CLASS

    }

    public enum Status {

            // service is stopped (not registred)
            STOPPED,
            // service is up and could be called by gateway
            STARTED,
            // service was stopped, and it should be removed from the memory
            CANCELLING,
            // service is registered but it is also down
            ZOMBIE

        ;

        public boolean isUp() {
            return this == STARTED;
        }

    }

}
