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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.httpRedirect.enabled=true",
    "apiml.service.externalUrl=https://localhost:40985",
    "apiml.health.protected=false"
})
class HttpsRedirectTest extends AcceptanceTestWithMockServices {

    @ParameterizedTest
    @CsvSource({"40985,/application/info", "10011,/application/info", "40985,/gateway/api/v1/auth/login"})
    void plaintextOnBothConnectorsRedirectsToTheConfiguredHttpsRoot(int connectorPort, String path) {
        given().redirects().follow(false)
            .header("Host", "untrusted.example")
            .header("X-Forwarded-Host", "untrusted.example")
            .when().get("http://localhost:" + connectorPort + path + "?test=1")
            .then().statusCode(308)
            .header("Location", equalTo("https://localhost:40985/"))
            .header("Strict-Transport-Security", nullValue())
            .body(equalTo(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void plaintextRequestsNeverReachTheBackend(String method) {
        var backend = mockService("redirecttest")
            .addEndpoint("/redirecttest/echo").body("backend response").and().start();

        given().redirects().follow(false)
            .when().request(method, "http://localhost:" + port + "/redirecttest/api/v1/echo?test=1")
            .then().statusCode(308)
            .header("Location", equalTo("https://localhost:40985/"));

        assertEquals(0, backend.getEndpoint().getCounter());
    }

    @ParameterizedTest
    @CsvSource({"40985,/application/info,200", "10011,/application/info,200",
        "40985,/gateway/api/v1/auth/query,401"})
    void httpsStillServesRequestsAndIncludesHsts(int connectorPort, String path, int status) {
        given().when().get("https://localhost:" + connectorPort + path)
            .then().statusCode(status)
            .header("Strict-Transport-Security", equalTo("max-age=31536000; includeSubDomains"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-age=300", "max-age=0"})
    void preservesBackendHstsPolicy(String policy) {
        var headers = new Headers();
        headers.set("Strict-Transport-Security", policy);
        mockService("redirecttest")
            .addEndpoint("/redirecttest/hsts").headers(headers).body("backend response").and().start();

        given().when().get(basePath + "/redirecttest/api/v1/hsts")
            .then().statusCode(200)
            .header("Strict-Transport-Security", equalTo(policy));
    }
}
