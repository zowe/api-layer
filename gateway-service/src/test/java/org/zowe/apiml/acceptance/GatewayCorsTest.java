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
public class GatewayCorsTest extends AcceptanceTestWithBasePath {
    // The behavior for gateway endpoints is the same isn't it?
    // Is there any simple request?
    // Where does it differ? Just in the way the behavior is handled?
    //   Ti definitely behaves differently in what is called and how. It will require


    // Basically from external point of view we have the same requests, but we don't need to prepare applications and
    // we need to prepare other services, probably zOSMF

    @Test
    // Verify the header to allow CORS isn't set
    void givenDefaultConfiguration_whenPreflightRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + "/api/v1/gateway/auth/login")
        .then()
            .statusCode(is(SC_FORBIDDEN))
            .header("Access-Control-Allow-Origin", is(nullValue()));
    }

    @Test
    // Verify the header to allow CORS isn't set
    void givenDefaultConfiguration_whenSimpleCorsRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .get(basePath + "/gateway/version")
        .then()
            .statusCode(is(SC_FORBIDDEN))
            .header("Access-Control-Allow-Origin", is(nullValue()));
    }



}
