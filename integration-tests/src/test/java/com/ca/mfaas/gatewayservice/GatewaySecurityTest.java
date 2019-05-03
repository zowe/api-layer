/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;

import com.ca.mfaas.utils.config.ConfigReader;
import io.restassured.RestAssured;

import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;

public class GatewaySecurityTest {
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private static final String PROTECTED_ENDPOINT = "/application/routes";
    private final static String COOKIE = "apimlAuthenticationToken";

    @Before
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    public void accessProtectedEndpointWithoutCredentials() {
        given()
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    @Test
    public void loginToGatewayAndAccessProtectedEndpointWithBasicAuthentication() {
        given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void loginToGatewayAndAccessProtectedEndpointWithCookie() {
        String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);

        given()
            .cookie(COOKIE, token)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void accessProtectedEndpointWithInvalidToken() {
        String invalidToken = "badToken";

        given()
            .cookie(COOKIE, invalidToken)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }


    @Test
    public void accessProtectedEndpointWithInvalidCredentials() {
        String invalidPassword = "badPassword";

        given()
            .auth().preemptive().basic(USERNAME, invalidPassword)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, PROTECTED_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }
    //@formatter:on
}
