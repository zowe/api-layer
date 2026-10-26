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

import com.sun.net.httpserver.Headers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.httpRedirect.enabled=true",
    "apiml.service.externalUrl=" + HttpsRedirectTest.REDIRECT_URL,
    "apiml.internal-discovery.port=18087",
    "apiml.health.protected=false"
})
class HttpsRedirectTest extends AcceptanceTestWithMockServices {

    static final String REDIRECT_URL = "https://localhost:40985/redirecttest/api/v1/landing?source=http";

    @ParameterizedTest
    @CsvSource({"40985,/application/info", "18087,/application/info", "40985,/gateway/api/v1/auth/login"})
    void plaintextOnBothConnectorsRedirectsToTheConfiguredHttpsUrl(int connectorPort, String path) {
        given().redirects().follow(false)
            .header("Host", "untrusted.example")
            .header("X-Forwarded-Host", "untrusted.example")
            .when().get("http://localhost:" + connectorPort + path + "?test=1")
            .then().statusCode(308)
            .header("Location", equalTo(REDIRECT_URL))
            .header("Strict-Transport-Security", nullValue())
            .body(equalTo(""));
    }

    @ParameterizedTest
    @ValueSource(ints = {40985, 18087})
    void clientFollowsRedirectToHttpsOnBothConnectors(int connectorPort) throws Exception {
        var backend = mockService("redirecttest")
            .addEndpoint("/redirecttest/landing")
            .assertion(exchange -> assertEquals("source=http", exchange.getRequestURI().getRawQuery()))
            .body("secure backend response")
            .and().start();

        var client = HttpClient.newBuilder()
            .sslContext(httpConfig.getSecureSslContextWithoutKeystore())
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + connectorPort + "/original/path?test=1"))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        var redirect = response.previousResponse().orElseThrow();
        assertEquals(308, redirect.statusCode());
        assertEquals(request.uri(), redirect.uri());
        assertEquals(REDIRECT_URL, redirect.headers().firstValue("Location").orElseThrow());
        assertTrue(redirect.headers().firstValue("Strict-Transport-Security").isEmpty());
        assertTrue(redirect.previousResponse().isEmpty());

        assertEquals(200, response.statusCode());
        assertEquals(URI.create(REDIRECT_URL), response.uri());
        assertEquals("secure backend response", response.body());
        assertTrue(response.headers().firstValue("Strict-Transport-Security").isPresent());
        assertEquals(1, backend.getEndpoint().getCounter());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void plaintextRequestsNeverReachTheBackend(String method) {
        var backend = mockService("redirecttest")
            .addEndpoint("/redirecttest/echo").body("backend response").and().start();

        given().redirects().follow(false)
            .when().request(method, "http://localhost:" + port + "/redirecttest/api/v1/echo?test=1")
            .then().statusCode(308)
            .header("Location", equalTo(REDIRECT_URL));

        assertEquals(0, backend.getEndpoint().getCounter());
    }

    @ParameterizedTest
    @CsvSource({"40985,/application/info,200", "18087,/application/info,200",
        "40985,/gateway/api/v1/auth/query,401"})
    void httpsStillServesRequestsAndIncludesHsts(int connectorPort, String path, int status) {
        given().when().get("https://localhost:" + connectorPort + path)
            .then().statusCode(status)
            .header("Strict-Transport-Security", equalTo("max-age=31536000; includeSubDomains"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-age=300", "max-age=0"})
    void overridesBackendHstsPolicy(String policy) {
        var headers = new Headers();
        headers.set("Strict-Transport-Security", policy);
        mockService("redirecttest")
            .addEndpoint("/redirecttest/hsts").headers(headers).body("backend response").and().start();

        given().when().get(basePath + "/redirecttest/api/v1/hsts")
            .then().statusCode(200)
            .header("Strict-Transport-Security", equalTo("max-age=31536000; includeSubDomains"));
    }
}
