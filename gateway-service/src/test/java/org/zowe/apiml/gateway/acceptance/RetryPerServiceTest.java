/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicroservicesAcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "apiml.gateway.servicesToDisableRetry=no-retry-service,no-RETRY-Service-2"
})
class RetryPerServiceTest extends AcceptanceTestWithMockServices {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    private MockService mockService;
    private MockService mockNoRetryService;
    private MockService mockNoRetryService2;

    @BeforeAll
    void startMockService() {
        mockService = mockService("serviceid1").scope(MockService.Scope.CLASS)
                .addEndpoint("/503").responseCode(503)
            .and()
                .addEndpoint("/401").responseCode(401)
            .and().start();

        mockNoRetryService = mockService("no-retry-service").scope(MockService.Scope.CLASS)
            .addEndpoint("/503").responseCode(503)
            .and().start();

        mockNoRetryService2 = mockService("No-Retry-Service-2").scope(MockService.Scope.CLASS)
            .addEndpoint("/503").responseCode(503)
            .and().start();
    }

    @Nested
    class GivenRetryOnAllOperationsIsDisabled {
        //Only default GET method remains active

        @Test
        void whenGetReturnsUnavailable_thenRetry() {
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid1")
            .when()
                .get(basePath + "/503")
            .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(6, mockService.getCounter());
        }

        @Test
        void whenRequestReturnsUnauthorized_thenDontRetry() {
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
        void whenPostReturnsUnavailable_thenDontRetry() {
            given()
                .header(HEADER_X_FORWARD_TO, "serviceid1")
            .when()
                .post(basePath + "/503")
            .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(1, mockService.getCounter());
        }

        @Test
        void whenRetryForServiceIsDisabled_andGetReturnsUnavailable_thenDontRetry() {
            given()
                .header(HEADER_X_FORWARD_TO, "no-retry-service")
                .when()
                .get(basePath + "/503")
                .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
            assertEquals(1, mockNoRetryService.getCounter());
        }

        @Test
        void whenRetryForServiceIsDisabled_andGetReturnsUnavailable_onMixedCaseServiceId_thenDontRetry() {
            given()
                .header(HEADER_X_FORWARD_TO, "no-retry-service-2")
                .when()
                .get(basePath + "/503")
                .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));

            assertEquals(1, mockNoRetryService2.getCounter());
        }

    }

}
