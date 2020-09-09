/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.restassured.RestAssured;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.gatewayservice.SecurityUtils;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

@Disabled
class LogoutTest {

    protected final static String LOGOUT_ENDPOINT = "/auth/logout";
    protected final static String QUERY_ENDPOINT = "/auth/query";
    protected final static String COOKIE_NAME = "apimlAuthenticationToken";

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    protected void assertIfLogged(String jwt, boolean logged) {
        final HttpStatus status = logged ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
        .when()
            .get(SecurityUtils.getGateWayUrl(QUERY_ENDPOINT))
        .then()
            .statusCode(status.value());
    }

    protected String generateToken() {
        return SecurityUtils.gatewayToken();
    }

    @Test
    void testLogout() {
        // make login
        String jwt = generateToken();

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
