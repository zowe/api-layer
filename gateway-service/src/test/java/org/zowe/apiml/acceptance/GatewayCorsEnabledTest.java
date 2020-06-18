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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithBasePath;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

// TODO: Parametrize for all the gateway endpoints
// TODO: Update and properly Mock or Stub needed dependencies
@AcceptanceTest
@ActiveProfiles("test")
public class GatewayCorsEnabledTest extends AcceptanceTestWithBasePath {
    // The behavior for gateway endpoints is the same isn't it?
    // Is there any simple request?
    // Where does it differ? Just in the way the behavior is handled?
    //   Ti definitely behaves differently in what is called and how. It will require

    // Basically from external point of view we have the same requests, but we don't need to prepare applications and
    // we need to prepare other services, probably zOSMF


    @Test
    // The CORS headers are properly set on the request
    void givenCorsIsAllowedForSpecificService_whenPreFlightRequestArrives_thenCorsHeadersAreSet() throws Exception {
        // Preflight request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + "/api/v1/gateway/auth/login")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin","https://foo.bar.org")
            .header("Access-Control-Allow-Methods", is("GET,HEAD,POST,DELETE,PUT,OPTIONS"))
            .header("Access-Control-Allow-Headers", is("origin, x-requested-with"));

        // Actual request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .get(basePath + "/api/v1/gateway/version")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", "https://foo.bar.org");
    }

    @Test @Disabled
    // The CORS header is properly set.
    // TODO: Is there any gateway related service with simple request?
    void givenCorsIsAllowedForSpecificService_whenSimpleRequestArrives_thenCorsHeadersAreSet() throws Exception {
        // Simple CORS request
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
        .when()
            .get(basePath + "/api/v1/gateway/auth/login")
        .then()
            .statusCode(is(SC_OK))
            .header("Access-Control-Allow-Origin", is("*"));
    }


}
