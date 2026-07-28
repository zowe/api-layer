/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.corsTests;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.restassured.http.Header;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService.MockServiceBuilder;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicroservicesAcceptanceTest
@TestPropertySource(properties = {
    "apiml.service.corsEnabled=true",
    "apiml.service.corsDefaultAllowedOrigins=https://foo.bar.org"
})
class CorsPerServiceTest extends AcceptanceTestWithMockServices {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

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
    void routeToServiceWithCorsEnabled() throws IOException {
        mockService("serviceid1")
            .addEndpoint("/test")
            .assertion(he -> assertNull(he.getRequestHeaders().getFirst(HttpHeaders.ORIGIN)))
        .and().start();

        given()
            .header(HttpHeaders.ORIGIN, "https://localhost:" + port)
            .header(HEADER_X_FORWARD_TO, "serviceid1")
        .when()
            .get(basePath + "/test")
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    // Verify the header to allow CORS isn't set
    // Verify there was no call to southbound service
    void givenCorsIsDelegatedToGatewayButServiceDoesntAllowCors_whenPreflightRequestArrives_thenDefaultCorsHeadersIsSet() {
        var headers = new Headers();
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicecors1", headers, Map.of("apiml.corsEnabled", "false"), assertions).start();

        given()
            .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
        .when()
            .options(basePath + "/servicecors1/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://foo.bar.org")
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "origin, x-requested-with")
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS");


        assertFalse(called.get());
    }

    @Test
    // Verify the header to allow CORS isn't set
    // Verify there was no call to southbound service
    void givenCorsIsDelegatedToGatewayButServiceDoesntAllowCors_whenSimpleCorsRequestArrives_thenDefaultCorsHeadersIsSet() {
        var headers = new Headers();
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicecors2", headers, Map.of("apiml.corsEnabled", "false"), assertions).start();

        given()
            .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
        .when()
            .post(basePath + "/servicecors2/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://foo.bar.org");

        assertTrue(called.get());
    }

    @Test
    // There is no request to the southbound server for preflight
    // There is request to the southbound server for the second request
    void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() {
        var headers = new Headers();
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicecors3", headers, Map.of("apiml.corsEnabled", "false"), assertions).start();

        // Preflight request
        given()
            .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
            .log().all()
        .when()
            .options(basePath + "/servicecors3/api/v1/fullheaders")
        .then()
            .log().all()
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
    // There is request to the southbound server for the request
    // The CORS header is properly set.
    void givenCorsIsAllowedForSpecificService_whenSimpleRequestArrives_thenCorsHeadersAreSet() {
        // There is request to the southbound server and the CORS headers are properly set on the response
        var headers = new Headers();
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicecors4", headers, Map.of("apiml.corsEnabled", "false"), assertions).start();

        // Preflight request
        given()
            .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org")) // This can't work anymore with the defaults (cors enabled on gateway + service with cors enabled + default list of origins)
        .when()
            .get(basePath + "/servicecors4/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is("https://foo.bar.org"));

        // The actual request is passed to the southbound service
        assertTrue(called.get() );
    }

}
