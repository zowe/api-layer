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
import com.sun.net.httpserver.HttpExchange;
import io.restassured.http.Header;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Modulith counterpart of {@code org.zowe.apiml.gateway.acceptance.corsTests.CorsPerServiceWildcardTest}.
 *
 * Verifies that a service configured with the exact wildcard origin is processed by Gateway CORS without the
 * Spring {@code IllegalArgumentException} caused by a literal {@code "*"} combined with credentials, and that
 * the requesting origin is echoed instead.
 */
@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.service.corsEnabled=true",
    "apiml.service.corsDefaultAllowedOrigins=https://foo.bar.org"
})
class CorsPerServiceWildcardTest extends AcceptanceTestWithMockServices {

    private static final String WILDCARD = "*";
    private static final String REQUESTING_ORIGIN = "https://bar.baz.org";
    private static final String PREFLIGHT_METHODS = "GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS";

    private MockServiceBuilder mockCorsService(String serviceId, Map<String, String> metadata, Collection<Consumer<HttpExchange>> assertions) {
        var builder = mockService(serviceId);
        var endpointBuilder = builder.addEndpoint("/" + serviceId + "/fullheaders");

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
    void givenServiceWildcardAndCredentialsEnabled_whenPreflightAndActualRequestArrive_thenRequestingOriginIsReturned() {
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().getFirst(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicewildcard1", Map.of(
            "apiml.corsEnabled", "true",
            "apiml.corsAllowedOrigins", WILDCARD
        ), assertions).start();

        // Preflight request
        given()
            .header(new Header(HttpHeaders.ORIGIN, REQUESTING_ORIGIN))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
        .when()
            .options(basePath + "/servicewildcard1/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(REQUESTING_ORIGIN))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(org.hamcrest.Matchers.not(WILDCARD)))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, is("true"))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, is(PREFLIGHT_METHODS));

        // The preflight request isn't passed to the southbound service
        assertFalse(called.get());

        // Actual request
        given()
            .header(new Header(HttpHeaders.ORIGIN, REQUESTING_ORIGIN))
        .when()
            .post(basePath + "/servicewildcard1/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(REQUESTING_ORIGIN))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, is("true"));

        // The actual request is passed to the southbound service
        assertTrue(called.get());
    }

    @Test
    void givenWildcardServiceAndExplicitService_whenUnlistedOriginCallsExplicitService_thenRejected() {
        mockCorsService("servicewildcard2", Map.of(
            "apiml.corsEnabled", "true",
            "apiml.corsAllowedOrigins", WILDCARD
        ), null).start();

        var explicitServiceCalled = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(httpExchange -> explicitServiceCalled.set(true));
        mockCorsService("serviceexplicit2", Map.of(
            "apiml.corsEnabled", "true",
            "apiml.corsAllowedOrigins", "https://allowed.example.com"
        ), assertions).start();

        given()
            .header(new Header(HttpHeaders.ORIGIN, REQUESTING_ORIGIN))
        .when()
            .get(basePath + "/serviceexplicit2/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_FORBIDDEN))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(nullValue()));

        assertFalse(explicitServiceCalled.get());
    }

    @Test
    void givenServiceWildcardAndCredentialsDisabled_whenPreflightArrives_thenWildcardAcceptedWithoutCredentials() {
        mockCorsService("servicewildcard3", Map.of(
            "apiml.corsEnabled", "true",
            "apiml.corsAllowedOrigins", WILDCARD,
            "apiml.corsAllowCredentials", "false"
        ), null).start();

        given()
            .header(new Header(HttpHeaders.ORIGIN, REQUESTING_ORIGIN))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
        .when()
            .options(basePath + "/servicewildcard3/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(REQUESTING_ORIGIN))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, is(nullValue()));
    }

    @Test
    void givenNoOriginHeader_whenRequestArrives_thenServiceIsCalledWithoutCorsHeaders() {
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().getFirst(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicewildcard4", Map.of(
            "apiml.corsEnabled", "true",
            "apiml.corsAllowedOrigins", WILDCARD
        ), assertions).start();

        given()
        .when()
            .get(basePath + "/servicewildcard4/api/v1/fullheaders")
        .then()
            .statusCode(is(SC_OK))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(nullValue()));

        assertTrue(called.get());
    }

}
