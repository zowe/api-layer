/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the redirect over real sockets through API ML's TLS connectors, security chains and service routing.
 */
@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.httpRedirect.enabled=true",
    "apiml.health.protected=false"
})
class HttpsRedirectTest extends AcceptanceTestWithMockServices {

    private HttpClient redirectingClient;

    @BeforeAll
    void createClient() {
        redirectingClient = HttpClient.newBuilder()
            .sslContext(httpConfig.getSecureSslContextWithoutKeystore())
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @ParameterizedTest
    @CsvSource({"40985,/application/info", "10011,/application/info", "40985,/gateway/api/v1/auth/login"})
    void plaintextIsRedirectedOnBothConnectorsBeforeAuthentication(int connectorPort, String path) {
        given().urlEncodingEnabled(false).redirects().follow(false)
            .header("Host", "untrusted.example")
            .when().get("http://localhost:" + connectorPort + path + "?test=a%2Fb")
            .then().statusCode(308)
            .header("Location", equalTo("https://localhost:" + connectorPort + path + "?test=a%2Fb"))
            .header("Strict-Transport-Security", nullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void plaintextRequestsRedirectBeforeReachingTheBackend(String method) {
        var backend = mockService("redirecttest")
            .addEndpoint("/redirecttest/echo").body("backend response").and().start();
        String path = "/redirecttest/api/v1/echo?test=a%2Fb";

        given().urlEncodingEnabled(false).redirects().follow(false)
            .header("Host", "untrusted.example")
            .header("Forwarded", "host=untrusted.example;proto=http")
            .header("X-Forwarded-Host", "untrusted.example")
            .header("X-Forwarded-Proto", "http")
            .when().request(method, "http://localhost:" + port + path)
            .then().statusCode(308)
            .header("Location", equalTo(basePath + path))
            .header("Strict-Transport-Security", nullValue())
            .body(equalTo(""));

        assertEquals(0, backend.getEndpoint().getCounter(), "Plaintext must not be routed to the service");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void followingRedirectPreservesTheRequestAndRoutesItOnce(String method) throws Exception {
        String body = "GET".equals(method) ? "" : "{\"message\":\"redirect payload ü\"}";
        String query = "test=a%2Fb&value=one%20two&repeat=first&repeat=second";
        String path = "/redirecttest/api/v1/encoded%20path?" + query;
        var backendRequests = new AtomicInteger();
        mockService("redirecttest")
            .addEndpoint("/redirecttest/encoded path")
            .assertion(exchange -> {
                backendRequests.incrementAndGet();
                assertAll(
                    () -> assertEquals(method, exchange.getRequestMethod()),
                    () -> assertEquals("/redirecttest/encoded%20path", exchange.getRequestURI().getRawPath()),
                    () -> assertEquals(query, exchange.getRequestURI().getRawQuery()),
                    () -> assertEquals(body, assertDoesNotThrow(() ->
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)))
                );
            })
            .body("backend response")
            .and().start();

        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
            .build();
        var response = redirectingClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("backend response", response.body());
        assertEquals(URI.create(basePath + path), response.uri());
        assertSingleRedirectWithHsts(response);
        assertEquals(1, backendRequests.get(), "Only the HTTPS request should reach the service");
        MockService.checkAssertionErrors();
    }

    @ParameterizedTest
    @CsvSource({"40985,/application/info,200", "10011,/application/info,200",
        "40985,/gateway/api/v1/auth/query,401"})
    void followingRedirectReachesHttpsOnTheCorrectConnector(int connectorPort, String path, int status) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + connectorPort + path))
            .timeout(Duration.ofSeconds(10)).GET().build();

        var response = redirectingClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(status, response.statusCode());
        assertEquals(URI.create("https://localhost:" + connectorPort + path), response.uri());
        assertSingleRedirectWithHsts(response);
    }

    @ParameterizedTest
    @CsvSource({"40985,/application/info,200", "10011,/application/info,200",
        "40985,/gateway/api/v1/auth/query,401"})
    void httpsStillServesRequestsAndAddsHstsAcrossSecurityChains(int connectorPort, String path, int status) {
        given().when().get("https://localhost:" + connectorPort + path)
            .then().statusCode(status)
            .header("Strict-Transport-Security", allOf(containsString("max-age=31536000"), containsString("includeSubDomains")));
    }

    private static void assertSingleRedirectWithHsts(HttpResponse<?> response) {
        var redirect = response.previousResponse().orElseThrow();
        assertEquals(308, redirect.statusCode());
        assertEquals("http", redirect.uri().getScheme());
        assertEquals(response.uri().toString(), redirect.headers().firstValue("Location").orElseThrow());
        assertTrue(redirect.previousResponse().isEmpty(), "There should be exactly one redirect");
        assertTrue(redirect.headers().allValues("Strict-Transport-Security").isEmpty());
        assertFalse(response.headers().firstValue("Location").isPresent());

        var hsts = response.headers().allValues("Strict-Transport-Security");
        assertEquals(1, hsts.size(), "The HTTPS response should contain one HSTS header");
        assertTrue(hsts.get(0).contains("max-age=31536000"));
        assertTrue(hsts.get(0).contains("includeSubDomains"));
    }
}
