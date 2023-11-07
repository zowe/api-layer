/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MockService;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.core.Is.is;

@AcceptanceTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "apiml.service.corsEnabled=false"
})
class RequestInstanceTest extends AcceptanceTestWithMockServices {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    @BeforeAll
    void setUp() throws IOException {
        mockService("serviceid1").scope(MockService.Scope.CLASS)
            .addEndpoint("/test")
            .and().start();
    }

    @Test
    void routeToCorrectService() {
        given()
            .header(HEADER_X_FORWARD_TO, "serviceid1")
        .when()
            .get(basePath + "/test")
        .then()
            .statusCode(Matchers.is(SC_OK));
    }

    @Test
    void routeToServiceWithCorsDisabled() {
        given()
            .header("Origin", "https://localhost:3000")
            .header(HEADER_X_FORWARD_TO, "serviceid1")
        .when()
            .get(basePath + "/test")
        .then()
            .statusCode(Matchers.is(SC_FORBIDDEN));
    }

    @Test
    void cantRouteToServer() {
        given()
            .header(HEADER_X_FORWARD_TO, "non-existing")
        .when()
            .get(basePath + "/test")
        .then()
            .statusCode(is(SC_NOT_FOUND));
    }

}
