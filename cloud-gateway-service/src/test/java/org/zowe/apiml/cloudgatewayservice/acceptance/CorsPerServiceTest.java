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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertNull;

@AcceptanceTest
class CorsPerServiceTest extends AcceptanceTestWithMockServices {

    private static final String HEADER_X_FORWARD_TO = "X-Forward-To";

    @Test
    void routeToServiceWithCorsEnabled() throws IOException {
        mockService("serviceid1")
            .addEndpoint("/test")
            .assertion(he -> assertNull(he.getRequestHeaders().getFirst("Origin")))
        .and().start();

        given()
            .header("Origin", "https://localhost:3000")
            .header(HEADER_X_FORWARD_TO, "serviceid1")
        .when()
            .get(basePath + "/test")
        .then()
            .statusCode(Matchers.is(SC_OK));
    }

}
