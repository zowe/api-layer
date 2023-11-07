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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MockService;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryPerServiceTest extends AcceptanceTestWithMockServices {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    private MockService mockService;

    @BeforeAll
    void startMockService() throws IOException {
        mockService = mockService("serviceid1").scope(MockService.Scope.CLASS)
                .addEndpoint("/503").responseCode(503)
            .and()
                .addEndpoint("/401").responseCode(401)
            .and().start();
    }

    @Nested
    class GivenRetryOnAllOperationsIsDisabled {

        @Test
        void whenGetReturnsUnavailable_thenRetry() throws Exception {
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid1")
            .when()
                .get(basePath + "/503")
            .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(6, mockService.getCounter());
        }

        @Test
        void whenRequestReturnsUnauthorized_thenDontRetry() throws Exception {
            for (int i = 1; i < 6; i++) {
                given()
                    .header(HEADER_X_FORWARD_TO, "serviceid1")
                .when()
                    .get(basePath + "/401")
                .then()
                    .statusCode(is(SC_UNAUTHORIZED));
                assertEquals(i, mockService.getCounter());
            }
        }

        @Test
        void whenPostReturnsUnavailable_thenDontRetry() throws Exception {
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid1")
            .when()
                .post(basePath + "/503")
            .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(1, mockService.getCounter());
        }

    }

}
