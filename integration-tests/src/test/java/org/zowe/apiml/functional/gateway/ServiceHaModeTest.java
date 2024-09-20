/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import org.apache.catalina.LifecycleException;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GatewayTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.RandomPorts;
import org.zowe.apiml.util.service.VirtualService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Objective is to test Gateway can retry on service that is down.
 * <p>
 * 2 services are registered and after that, one is killed. Service is called through Gateway
 * and responses are inspected. Implementation returns a debug header that describes the retries.
 * The test repeats calls until it sees that request has been retried from mentioned header.
 */
@TestsNotMeantForZowe
@GatewayTest
class ServiceHaModeTest implements TestWithStartedInstances {

    private static final int TIMEOUT = 30;

    @BeforeAll
    static void setUp() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    private static Stream<Arguments> retryableHttpMethods() {
        return Stream.of(
            Arguments.of(Method.GET)
        );
    }

    private static Stream<Arguments> nonRetryableHttpMethods() {
        return Stream.of(
            Arguments.of(Method.POST),
            Arguments.of(Method.PUT),
            Arguments.of(Method.DELETE),
            Arguments.of(Method.OPTIONS)
        );
    }

    static Stream<Arguments> httpMethods() {
        return Stream.concat(retryableHttpMethods(), nonRetryableHttpMethods());
    }

    @Nested
    class GivenTwoServices {

        private VirtualService service1;
        private VirtualService service2;

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenOneIsDown {

            @BeforeAll
            void setUp() throws LifecycleException, IOException, JSONException {
                List<Integer> ports = RandomPorts.generateUniquePorts(2);

                service1 = new VirtualService("testHaModeService1", ports.get(0));
                service2 = new VirtualService("testHaModeService1", ports.get(1));

                service1.start();
                service2.start().waitForGatewayRegistration(TIMEOUT);

                service2.zombie();
            }

            @AfterAll
            void cleanUp() {
                try {
                    service1.close();
                } catch (Exception e) {
                    // Just continue
                }
                try {
                    service2.close();
                } catch (Exception e) {
                    // Just continue
                }
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.functional.gateway.ServiceHaModeTest#httpMethods")
            void verifyThatGatewayRetriesToTheLiveOne(Method method) {
                routeAndVerifyRetry(service1.getGatewayUrls(), method, TIMEOUT);
            }

            // TODO This method used to verify how many retries happened based on an optional response header with debug information
            private void routeAndVerifyRetry(List<String> gatewayUrls, Method method, int timeoutSec) {
                final long time0 = System.currentTimeMillis();

                for (String gatewayUrl : gatewayUrls) {
                    while (true) {
                        String url = gatewayUrl + "/application/instance";

                        try {
                            Response response = doRequest(method, url);
                            assertEquals(HttpStatus.SC_OK, response.getStatusCode());
                            break;
                        } catch (RuntimeException | AssertionError e) {
                            if (System.currentTimeMillis() - time0 > timeoutSec * 1000L) throw e;
                            await().timeout(1, TimeUnit.SECONDS);
                        }
                    }
                }
            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class OneReturns503 {

            @BeforeAll
            void setUp() throws LifecycleException, IOException, JSONException {
                List<Integer> ports = RandomPorts.generateUniquePorts(2);

                service1 = new VirtualService("testHaModeService2", ports.get(0));
                service2 = new VirtualService("testHaModeService2", ports.get(1));

                service1.addInstanceServlet("Http503", "/httpCode");
                service2.addHttpStatusCodeServlet(HttpStatus.SC_SERVICE_UNAVAILABLE);

                service1.start();
                service2.start().waitForGatewayRegistration(TIMEOUT);
            }

            @AfterAll
            void cleanUp() {
                try {
                    service1.close();
                } catch (Exception e) {
                    // Just continue
                }
                try {
                    service2.close();
                } catch (Exception e) {
                    // Just continue
                }
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.functional.gateway.ServiceHaModeTest#retryableHttpMethods")
            void verifyThatGatewayRetriesGet(Method method) {
                routeAndVerifyRetries(service1.getGatewayUrls(), method);
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.functional.gateway.ServiceHaModeTest#nonRetryableHttpMethods")
            void verifyThatGatewayNotRetriesPost(Method method) {
                routeAndVerifyRetries(service1.getGatewayUrls(), method);
            }

            // TODO This method used to verify how many retries happened based on an optional response header with debug information
            private void routeAndVerifyRetries(List<String> gatewayUrls, Method method) {
                for (String gatewayUrl : gatewayUrls) {
                    IntStream.rangeClosed(0, 1).forEach(x -> {

                        String url = gatewayUrl + "/httpCode";
                        Response response = doRequest(method, url);
                        if (response.getStatusCode() != HttpStatus.SC_OK && response.getStatusCode() != HttpStatus.SC_SERVICE_UNAVAILABLE) {
                            fail("Return should be 200 or 503 but it is: " + response.getStatusCode());
                        }
                    });
                }
            }

        }

    }

    private Response doRequest(Method method, String url) {
        return when()
            .request(method, url)
            .andReturn();
    }

}
