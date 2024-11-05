/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "apiml.security.filterChainConfiguration=new",
        "apiml.health.protected=true"
    }
)
class ZaasExceptionHandlerTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void init() {
        RestAssured.baseURI = "https://localhost";
        RestAssured.port = port;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenUnknownEndpoint_whenCallZaas_thenReturns404WithMessage() {
        given().when()
            .get("/unknown/endpoint")
        .then()
            .statusCode(404)
            .body("messages[0].messageKey", is("org.zowe.apiml.common.endPointNotFound"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/zaas/scheme/ticket",
        "/application/health"
    })
    void givenNoCredentials_whenCallZaas_thenReturns401WithMessage(String url) {
        given().when()
            .get(url)
        .then()
            .statusCode(401)
            .body("messages[0].messageKey", is("org.zowe.apiml.security.authRequired"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/zaas/api/v1/auth/login"
    })
    void givenNoCredentials_whenLogin_thenReturns401WithMessage(String url) {
        given().when()
            .auth().preemptive().basic("UNKNOWN", "WRONG")
            .post(url)
        .then()
            .statusCode(401)
            .body("messages[0].messageKey", is("org.zowe.apiml.security.login.invalidCredentials"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/zaas/api/v1/auth/oidc/webfinger" // missing ?resource=abc
    })
    void givenNoRequiredArgument_whenCallZaas_thenReturns400WithMessage(String url) {
        given().when()
            .get(url)
        .then()
            .statusCode(400)
            .body("messages[0].messageKey", is("org.zowe.apiml.common.badRequest"));
    }

}
