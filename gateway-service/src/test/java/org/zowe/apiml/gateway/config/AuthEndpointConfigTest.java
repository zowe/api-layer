/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MockService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthEndpointConfigTest extends AcceptanceTestWithMockServices {

    @BeforeAll
    void setup() throws IOException {
        mockService("zaas").scope(MockService.Scope.CLASS)
            .addEndpoint("/zaas/api/v1/auth/login")
                .responseCode(204)
                .assertion(he -> assertEquals("Basic dXNlcjpwYXNz", he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .assertion(he -> assertEquals("POST", he.getRequestMethod()))
                .and()
            .addEndpoint("/zaas/api/v1/auth/query")
                .responseCode(200)
                .assertion(he -> assertEquals("{\"input\":\"question\"}", getBody(he)))
                .contentType(APPLICATION_JSON)
                .body("{\"status\":\"valid\"}")
                .and()
            .addEndpoint("/zaas/api/v1/auth/access-token/revoke")
                .responseCode(405)
                .assertion(he -> assertEquals("GET", he.getRequestMethod()))
            .and().start();
    }

    private String getBody(HttpExchange he) {
        try {
            return IOUtils.toString(he.getRequestBody(), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            fail(ioe);
            return null;
        }
    }

    @Test
    void givenAuthorizationHeader_whenCallLoginEndpoint_thenRedirect() {
        given()
            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
        .when()
            .post(basePath + "/gateway/api/v1/auth/login")
        .then()
            .statusCode(Matchers.is(SC_NO_CONTENT));
    }

    @Test
    void givenAuthorizationCall_whenReturnABody_thenResponseHasPayload() {
        given()
            .body("{\"input\":\"question\"}")
        .when()
            .get(basePath + "/gateway/api/v1/auth/query")
        .then()
            .statusCode(Matchers.is(SC_OK))
            .body("status", is("valid"));
    }

    @Test
    void givenUnkwnownAddress_whenCallGateway_thenReturn404() {
        given()
            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
        .when()
            .post(basePath + "/gateway/api/v1/unknown")
        .then()
            .statusCode(Matchers.is(SC_NOT_FOUND));
    }

    @Test
    void givenAWrongMethodName_whenCallAuthEndpoint_thenReturn405() {
        given()
        .when()
            .get(basePath + "/gateway/api/v1/auth/access-token/revoke")
        .then()
            .statusCode(Matchers.is(SC_METHOD_NOT_ALLOWED));
    }

}
