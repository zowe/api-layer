/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.categories.MainframeDependentTests;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

@MainframeDependentTests
public class LogoutTest {

    private final static String BASE_PATH = "/api/v1/gateway";
    private final static String LOGOUT_ENDPOINT = "/auth/logout";
    private final static String QUERY_ENDPOINT = "/auth/query";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    private void assertIfLogged(String jwt, boolean logged) {
        final HttpStatus status = logged ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
        .when()
            .get(SecurityUtils.getGateWayUrl(QUERY_ENDPOINT))
        .then()
            .statusCode(status.value());
    }

    @Test
    public void testLogout() {
        // make login
        String jwt = SecurityUtils.gatewayToken();

        // check if it is logged in
        assertIfLogged(jwt, true);

        // make logout
        given()
            .cookie(COOKIE_NAME, jwt)
        .when()
            .post(SecurityUtils.getGateWayUrl(LOGOUT_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT));

        // check if it is logged in
        assertIfLogged(jwt, false);
    }

}
