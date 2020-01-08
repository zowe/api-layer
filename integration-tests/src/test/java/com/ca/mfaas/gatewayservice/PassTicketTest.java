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

import static com.ca.mfaas.gatewayservice.SecurityUtils.GATEWAY_TOKEN_COOKIE_NAME;
import static com.ca.mfaas.gatewayservice.SecurityUtils.gatewayToken;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;

import com.ca.mfaas.util.config.ConfigReader;

import org.junit.Before;
import org.junit.Test;

import io.restassured.RestAssured;

public class PassTicketTest {
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
            .getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private final static String STATICCLIENT_BASE_PATH = "/api/v1/staticclient";
    private final static String PASSTICKET_TEST_ENDPOINT = "/passticketTest";

    @Before
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void accessServiceWithCorrectPassTicket() {
        String jwt = gatewayToken();
        given().cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt).when().get(
                String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
                .then().statusCode(is(SC_OK));
    }

    @Test
    public void accessServiceWithIncorrectApplId() {
        String jwt = gatewayToken();
        given().cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt).when()
                .get(String.format("%s://%s:%d%s%s?applId=XBADAPPL", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH,
                        PASSTICKET_TEST_ENDPOINT))
                .then().statusCode(is(SC_INTERNAL_SERVER_ERROR))
                .body("message", containsString("Error on evaluation of PassTicket"));
    }

    @Test
    //@formatter:off
    public void accessServiceWithIncorrectToken() {
        String jwt = "nonsense";
        String expectedMessage = "Token is not valid";

        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAG102E' }.messageContent", equalTo(expectedMessage));
    }
    //@formatter:on
}
