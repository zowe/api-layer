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
import io.restassured.http.Cookie;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.service.DiscoveryUtils;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

public class LogoutTest {

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private final static String BASE_PATH = "/api/v1/gateway";
    private final static String LOGIN_ENDPOINT = "/auth/login";
    private final static String LOGOUT_ENDPOINT = "/auth/logout";
    private final static String QUERY_ENDPOINT = "/auth/query";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";

    @Before
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    private String getJwt() {
        Cookie cookie = given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
        .when()
            .post(String.format("%s%s%s", DiscoveryUtils.getGatewayUrls().get(0), BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);
        return cookie.getValue();
    }

    private void assertIfLogged(String jwt, boolean logged) {
        final HttpStatus status = logged ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;

        DiscoveryUtils.getGatewayUrls().forEach(gw -> {
            given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .when()
                .get(String.format("%s%s%s", gw, BASE_PATH, QUERY_ENDPOINT))
            .then()
                .statusCode(status.value());
        });
    }

    @Test
    public void testLogout() {
        // make login
        String jwt = getJwt();

        // check if it is logged in
        assertIfLogged(jwt, true);

        // make logout
        given()
            .cookie(COOKIE_NAME, jwt)
        .when()
            .post(String.format("%s%s%s", DiscoveryUtils.getGatewayUrls().get(0), BASE_PATH, LOGOUT_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT));

        // check if it is logged in
        assertIfLogged(jwt, false);
    }

}
