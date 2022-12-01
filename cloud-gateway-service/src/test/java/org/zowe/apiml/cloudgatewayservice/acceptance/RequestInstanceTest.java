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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;

@AcceptanceTest
public class RequestInstanceTest extends AcceptanceTestWithTwoServices {
    @Nested
    class WhenValidInstanceId {

        @BeforeEach
        void setUp() throws IOException {
            mockServerWithSpecificHttpResponse(200, "serviceid1", 4000);
        }

        @Test
        void routeToCorrectService() {
            given()
                .header("X-Request-Id", "serviceid2localhost")
                .when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then().statusCode(Matchers.is(SC_OK));
        }
    }

    @Nested
    class WhenNonExistingInstanceId {
        @Test
        void cantRouteToServer() {
            given()
                .header("X-Request-Id", "non-existing").
                when()
                .get(basePath + serviceWithCustomConfiguration.getPath())
                .then()
                .statusCode(is(SC_NOT_FOUND));
        }
    }
}
