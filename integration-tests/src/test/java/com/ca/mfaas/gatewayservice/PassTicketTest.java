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

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import com.ca.apiml.security.common.login.LoginRequest;
import com.ca.mfaas.util.config.ConfigReader;

import org.junit.Before;
import org.junit.Test;

import io.restassured.RestAssured;
import io.restassured.http.Cookie;

public class PassTicketTest {
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private final static String GATEWAY_BASE_PATH = "/api/v1/gateway";
    private final static String STATICCLIENT_BASE_PATH = "/api/v1/staticclient";
    private final static String LOGIN_ENDPOINT = "/auth/login";
    private final static String PASSTICKET_TEST_ENDPOINT = "/passticketTest";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();

    @Before
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private String login() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        Cookie cookie = given().contentType(JSON).body(loginRequest).when()
                .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, GATEWAY_BASE_PATH, LOGIN_ENDPOINT)).then()
                .statusCode(is(SC_NO_CONTENT)).cookie(COOKIE_NAME, not(isEmptyString())).extract()
                .detailedCookie(COOKIE_NAME);

        assertThat(cookie.getValue(), is(notNullValue()));

        return cookie.getValue();
    }

    @Test
    public void accessServiceWithCorrectPassTicket() {
        String jwt = login();
        given().cookie(COOKIE_NAME, jwt).when().get(
                String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
                .then().statusCode(is(SC_OK));
    }
}
