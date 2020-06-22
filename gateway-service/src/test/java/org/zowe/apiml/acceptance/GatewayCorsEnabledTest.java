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

import io.restassured.http.Header;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithBasePath;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

@AcceptanceTest
@ActiveProfiles("test")
@DirtiesContext
class GatewayCorsEnabledTest extends AcceptanceTestWithBasePath {
    @Test
    // The CORS headers are properly set on the request
    void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() throws Exception {
        // Preflight request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + "/gateway/version")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin","https://foo.bar.org")
            .header("Access-Control-Allow-Methods", "GET,HEAD,POST,DELETE,PUT,OPTIONS")
            .header("Access-Control-Allow-Headers", "origin, x-requested-with");

        // Actual request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .get(basePath + "/gateway/version")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", "https://foo.bar.org");
    }



}
