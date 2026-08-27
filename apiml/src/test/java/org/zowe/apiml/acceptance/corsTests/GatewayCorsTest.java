/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.corsTests;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.restassured.http.Header;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.AcceptanceTest;
import org.zowe.apiml.acceptance.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.MockService.MockServiceBuilder;
import org.zowe.apiml.gateway.MockService.Scope;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modulith counterpart of {@code org.zowe.apiml.gateway.acceptance.corsTests.GatewayCorsTest}.
 */
@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
class GatewayCorsTest {

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "apiml.health.protected=false",
        "apiml.service.corsDefaultAllowedOrigins=https://foo.bar.org",
        "apiml.service.corsEnabled=true"
    })
    class GatewayCorsEnabledWithProvidedDefaultTest extends AcceptanceTestWithMockServices {

        private MockServiceBuilder mockCorsService(String serviceId, Headers responseHeaders, Map<String, String> metadata, Collection<Consumer<HttpExchange>> assertions) {
            var builder = mockService(serviceId);
            var endpointBuilder = builder.addEndpoint("/" + serviceId + "/fullheaders");

            if (responseHeaders != null) {
                endpointBuilder.headers(responseHeaders);
            }
            if (metadata != null) {
                builder.additionalMetadata(metadata);
            }
            if (assertions != null) {
                endpointBuilder.assertions(assertions);
            }

            return endpointBuilder
                .and()
            .scope(Scope.TEST);
        }

        @Test
        // The CORS headers are properly set on the request
        void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() {
            // Preflight request
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
            .when()
                .options(basePath + "/gateway/version")
            .then()
                .statusCode(is(SC_OK))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,"https://foo.bar.org")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS")
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, x-requested-with");

            // Actual request
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .when()
                .get(basePath + "/gateway/version")
            .then()
                .statusCode(is(SC_OK))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://foo.bar.org");
        }

        @Test
        void givenCorsOriginIsNotAllowed_whenPreFlightRequestArrives_thenCorsHeadersAreNotSet() {
            mockCorsService("servicecors1", null, Map.of("apiml.corsEnabled", "true"), null).start();

            // Preflight request with disallowed origin
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://malicious.example.com"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
            .when()
                .options(basePath + "/servicecors1/api/v1/fullheaders")
            .then()
                .statusCode(is(SC_FORBIDDEN))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is((String) null));
        }

        @Test
        // There is request to the southbound server for the request
        // The CORS header is properly set.
        void givenCorsIsAllowedForSpecificService_whenSimpleRequestArrives_thenCorsHeadersAreSetAndOnlyTheOnesByGateway() {
            var headers = new Headers();
            var called = new AtomicBoolean(false);
            List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    called.set(true);
                }
            );

            mockCorsService("servicecors2", headers, Map.of("apiml.corsEnabled", "true"), assertions).start();
            // There is request to the southbound server and the CORS headers are properly set on the response
            // Preflight request
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .when()
                .get(basePath + "/servicecors2/api/v1/fullheaders")
            .then()
                .statusCode(is(SC_OK))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is("https://foo.bar.org"));

            // The actual request is passed to the southbound service
            assertTrue(called.get());
        }

        @Test
        // There is no request to the southbound server for preflight
        // There is request to the southbound server for the second request
        void givenCorsIsAllowedForSpecificService_whenTheServiceIsSet_thenCorsHeadersAreSetAndOnlyTheOnesByGateway() {
            var headers = new Headers();

            var called = new AtomicBoolean(false);
            List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    called.set(true);
                }
            );

            mockCorsService("servicecors3", headers, Map.of("apiml.corsEnabled", "true"), assertions).start();

            // Preflight request
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
            .when()
                .options(basePath + "/servicecors3/api/v1/fullheaders")
            .then()
                .statusCode(is(SC_OK))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is("https://foo.bar.org"))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, is("GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS"))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, is("origin, x-requested-with"));

            // The preflight request isn't passed to the southbound service
            assertFalse(called.get());

            // Actual request
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .when()
                .post(basePath + "/servicecors3/api/v1/fullheaders")
            .then()
                .statusCode(is(SC_OK))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is("https://foo.bar.org"));

            // The actual request is passed to the southbound service
            assertTrue(called.get());
        }

        @Test
        void givenCorsIsEnabled_whenRequestWithOriginComes_thenOriginIsntPassedToSouthbound() {
            // There is request to the southbound server and the CORS headers are properly set on the response
            var headers = new Headers();

            var called = new AtomicBoolean(false);
            List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    called.set(true);
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                }
            );

            mockCorsService("servicecors4", headers, Map.of("apiml.corsEnabled", "true"), assertions).start();

            // Simple request
            given()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .when()
                .get(basePath + "/servicecors4/api/v1/fullheaders")
            .then()
                .statusCode(is(SC_OK))
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is("https://foo.bar.org"));

            // The actual request is passed to the southbound service
            assertTrue(called.get());
        }

    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(properties = {
        "apiml.service.corsEnabled=true"
    })
    class GatewayCorsEnabledWithDefaultsTest extends AcceptanceTestWithMockServices {

        private MockServiceBuilder mockCorsService(String serviceId, Headers responseHeaders, Map<String, String> metadata, Collection<Consumer<HttpExchange>> assertions) {
            var builder = mockService(serviceId);
            var endpointBuilder = builder.addEndpoint("/" + serviceId + "/fullheaders");

            if (responseHeaders != null) {
                endpointBuilder.headers(responseHeaders);
            }
            if (metadata != null) {
                builder.additionalMetadata(metadata);
            }
            if (assertions != null) {
                endpointBuilder.assertions(assertions);
            }

            return endpointBuilder
                .and()
            .scope(Scope.TEST);
        }
        // Gateway uses a default list of origins, does not accept any (*)

        @Test
        void givenCorsIsEnabledWithDefaults_whenPreflightRequestComes_thenPreflightIsRejected() {
            var headers = new Headers();

            var called = new AtomicBoolean(false);
            List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );

            mockCorsService("servicecors5", headers, Map.of("apiml.corsEnabled", "true"), assertions).start();

            // Preflight request with origin that should be rejected by default CORS policy
            given()
                .log().all()
                .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .when()
                .options(basePath + "/servicecors5/api/v1/fullheaders")
            .then()
                .log().all()
                .statusCode(is(SC_FORBIDDEN));

            // No request should be passed to the southbound service for preflight
            assertFalse(called.get());
        }

        @Test
        void givenCorsIsEnabledWithDefaults_whenPreflightRequestWithLocalhostOriginComes_thenPreflightIsAccepted() {
            var headers = new Headers();

            var called = new AtomicBoolean(false);
            List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );

            mockCorsService("servicecors6", headers, Map.of("apiml.corsEnabled", "true"), assertions).start();
            // Preflight request with localhost origin that should be accepted by default CORS policy
            given()
                .log().all()
                .header(new Header(HttpHeaders.ORIGIN, "https://localhost:" + port))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .when()
                .options(basePath + "/servicecors6/api/v1/fullheaders")
            .then()
            .log().all()
                .statusCode(is(SC_OK));

            // No request should be passed to the southbound service for preflight
            assertFalse(called.get());
        }

    }

}
