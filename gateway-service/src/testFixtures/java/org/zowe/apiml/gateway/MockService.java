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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Joiner;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.PortType;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.assertj.core.error.MultipleAssertionsError;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.http.MediaType;
import org.zowe.apiml.auth.AuthenticationScheme;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class allows to mock any simply service to a functional test. It is fully integrated in
 * AcceptanceTestWithMockServices. In case you are using directly this implementation DO NOT FORGET to close
 * the service once it is released and in the similar way, without AcceptanceTestWithMockServices it is
 * necessary to mock registry and routing. The easiest way is to use the method
 * @link AcceptanceTestWithMockServices#mockService(String). It allows to you to use the same features and also
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
@Slf4j
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class MockService implements AutoCloseable {

    protected static int idCounter = 1;
    // in case on zombie mode is necessary to have a unique port number, on start replaced with the real one
    protected int port;

    /**
     * HTTP server to handle requests and the endpoint configuration
     */
    @Getter(AccessLevel.NONE)
    private HttpServer server;
    @Getter(AccessLevel.NONE)
    protected List<Endpoint> endpointsConfig;

    /**
     * Service identification
     */
    protected String serviceId;
    protected String vipAddress;
    @Builder.Default
    protected String hostname = "localhost";

    /**
     * Routing configuration
     */
    protected String gatewayUrl;
    protected String serviceUrl;

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
    protected Scope scope = Scope.TEST;

    @Singular
    @Getter(AccessLevel.NONE)
    protected List<Consumer<MockService>> statusChangedlisteners;

    /**
     * All registered endpoints. It is possible to get any instance by path. If there is just one endpoint in the
     * service, you can use {@link MockService#getEndpoint()}
     */
    protected final Map<String, Endpoint> endpoints = new HashMap<>();

    /**
     * Additional metadata added on top of standard one required for the mock service to run
     */
    protected Map<? extends String, ? extends String> additionalMetadata;

    /**
     * Status of the service - see possible values {@link MockService.Status}
     */
    @Getter(AccessLevel.NONE)
    protected final AtomicReference<Status> status = new AtomicReference<>(Status.STOPPED);

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

    static void setAssertionError(AssertionError assertionError) {
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

    void setStatus(Status status) {
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
     *
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

    Map<String, String> getMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.routes.api-v1.gatewayUrl", "api/v1");
        metadata.put("apiml.routes.api-v1.serviceUrl", "/" + serviceId);

        if (authenticationScheme != null) {
            metadata.put("apiml.authentication.scheme", authenticationScheme.getScheme());
        }
        if (applid != null) {
            metadata.put("apiml.authentication.applid", applid);
        }

        metadata.putAll(additionalMetadata);

        return metadata;
    }

    /**
     * Construct InstanceInfo for the mock service
     *
     * @return instanceInfo with all related data
     */
    public InstanceInfo.Builder getInstanceInfo() {
        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(getInstanceId())
            .setHostName(hostname)
            .setPort(port)
            .enablePort(PortType.SECURE, false)
            .enablePort(PortType.UNSECURE, true)
            .setAppName(serviceId)
            .setVIPAddress(vipAddress != null ? vipAddress : serviceId)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(getMetadata());
    }

    /**
     * Method call {@link MockService#getInstanceInfo()} converted to EurekaServiceInstance
     *
     * @return EurekaServiceInstance with all related data
     */
    public EurekaServiceInstance getEurekaServiceInstance() {
        InstanceInfo instanceInfo = getInstanceInfo().build();
        return instanceInfo == null ? null : new EurekaServiceInstance(instanceInfo);
    }

    public static class MockServiceBuilder {

        private List<Endpoint> endpoints = new LinkedList<>();
        private Map<String, String> additionalMetadata = new HashMap<>();

        /**
         * Create a new endpoint of the Mock Service
         *
         * @param path Path of the endpoint
         * @return builder to define other values
         */
        public Endpoint.EndpointBuilder addEndpoint(String path) {
            Endpoint.EndpointBuilder endpointBuilder = Endpoint.builder();
            if (!path.startsWith("/")) {
                throw new IllegalArgumentException();
            }
            endpointBuilder.path(path);
            endpointBuilder.mockServiceBuilder = this;
            return endpointBuilder;
        }

        /**
         * To build mock service. It will be stopped (not registred). It is necessary to call method start or zombie.
         *
         * @return instance of mockService
         */
        public MockService build() {
            MockService mockService = internalBuild();
            mockService.port = idCounter++;
            mockService.endpointsConfig = endpoints;
            mockService.additionalMetadata = additionalMetadata;
            return mockService;
        }

        /**
         * To start build and start MockService
         *
         * @return instance of MockService
         * @throws IOException - in case of any issue with starting server
         */
        public MockService start() {
            MockService mockService = build();
            try {
                mockService.start();
            } catch (RuntimeException | IOException e) {
                int i = atCounter.getAndIncrement();
                log.info("Not able to start mock server. Number of retries: {}", i);
                if (i < 4) {
                    start();
                }
            }
            atCounter.set(0);
            return mockService;
        }

        AtomicInteger atCounter = new AtomicInteger(0);

    }

    @Builder
    @Value
    public static class Endpoint {

        /**
         * The default response, used unless {@link #responseProvider} is set.
         */
        private Response response;

        /**
         * Path of the endpoint
         */
        private String path;

        /**
         * Lambdas about assertion on server side. The outcome exception could be thrown by
         * {@link MockService#checkAssertionErrors()}
         */
        @Singular
        protected List<Consumer<HttpExchange>> assertions;

        /**
         * Counter of calls. It contains amount of received requests.
         */
        @Builder.Default
        private AtomicInteger counter = new AtomicInteger();

        /**
         * Computes the response from the request itself, instead of the static {@link #response}.
         * Useful when the response needs to vary per call.
         */
        private Function<HttpExchange, Response> responseProvider;

        void process(HttpExchange httpExchange) throws IOException {
            try {
                Response response = getResponse(httpExchange);

                if (response.getContentType() != null) {
                    httpExchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, response.getContentType());
                }

                if (assertions != null) {
                    assertions.forEach(assertion -> {
                        try {
                            assertion.accept(httpExchange);
                        } catch (AssertionError afe) {
                            setAssertionError(afe);
                        }
                    });
                }

                byte[] bodyBytes = response.getBody() == null ? null : response.getBody().getBytes(StandardCharsets.UTF_8);

                log.debug("Request headers: " + Joiner.on(",").withKeyValueSeparator("=").join(httpExchange.getRequestHeaders()));
                log.debug("Response headers: " + Joiner.on(",").withKeyValueSeparator("=").join(httpExchange.getResponseHeaders()));

                for (Map.Entry<String, List<String>> headerEntry : response.getHeaders().entrySet()) {
                    for (String value : headerEntry.getValue()) {
                        httpExchange.getResponseHeaders().add(headerEntry.getKey(), value);
                    }
                }
                httpExchange.sendResponseHeaders(response.getResponseCode(), bodyBytes == null ? 0 : bodyBytes.length);

                if (bodyBytes != null) {
                    try (OutputStream os = httpExchange.getResponseBody()) {
                        os.write(bodyBytes);
                    }
                }
            } finally {
                counter.getAndIncrement();
                httpExchange.close();
            }
        }

        private Response getResponse(HttpExchange httpExchange) {
            return responseProvider != null ? responseProvider.apply(httpExchange) : response;
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

            private final Response.ResponseBuilder responseBuilder = Response.builder();

            /**
             * Response code of a response, as default 200
             */
            public EndpointBuilder responseCode(int responseCode) {
                responseBuilder.responseCode(responseCode);
                return this;
            }

            /**
             * Content type of the response. As default null (no header is generated).
             */
            public EndpointBuilder contentType(String contentType) {
                responseBuilder.contentType(contentType);
                return this;
            }

            /**
             * Added response headers
             */
            public EndpointBuilder headers(Headers headers) {
                responseBuilder.headers(headers);
                return this;
            }

            /**
             * Response body to answer
             */
            public EndpointBuilder body(String body) {
                responseBuilder.body(body);
                return this;
            }

            /**
             * To set body and content type to application/json
             *
             * @param body object to be converted to the json (to be returned in the response)
             * @return builder of the endpoint
             * @throws JsonProcessingException in case an issue with generation of JSON
             */
            public EndpointBuilder bodyJson(Object body) throws JsonProcessingException {
                if (body == null) {
                    return body(null);
                } else if (body instanceof String b) {
                    return body(b);
                } else {
                    ObjectWriter writer = new ObjectMapper().writer();
                    contentType(MediaType.APPLICATION_JSON_VALUE);
                    return body(writer.writeValueAsString(body));
                }
            }

            /**
             * Definition of the endpoint is done, continue with defining of the MockService
             *
             * @return instance of MockService's builder
             */
            public MockServiceBuilder and() {
                response(responseBuilder.build());
                Endpoint endpoint = build();
                mockServiceBuilder.endpoints.add(endpoint);
                return mockServiceBuilder;
            }

        }

        @Builder
        @Value
        public static class Response {

            @Builder.Default
            private int responseCode = 200;

            private String contentType;

            @Builder.Default
            private Headers headers = new Headers();

            private String body;

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
