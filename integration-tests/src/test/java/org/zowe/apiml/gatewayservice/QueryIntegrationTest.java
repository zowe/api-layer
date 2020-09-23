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
import org.junit.jupiter.api.*;
import org.zowe.apiml.util.config.ConfigReader;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

public class QueryIntegrationTest {
    private final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private final static String BASE_PATH = "/api/v1/gateway";
    private final static String QUERY_ENDPOINT = "/auth/query";
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String COOKIE = "apimlAuthenticationToken";

    private String token;

    @BeforeEach
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.basePath = BASE_PATH;
        RestAssured.useRelaxedHTTPSValidation();

        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    @AfterEach
    public void tearDown() {
    }

    //@formatter:off
    @Test
    void doQueryWithValidTokenFromHeader() {
        given()
             .header("Authorization", "Bearer " + token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body("userId", equalTo(USERNAME));
    }

    @Test
    void doQueryWithValidTokenFromCookie() {
        given()
            .cookie(COOKIE, token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body("userId", equalTo(USERNAME));
    }

    @Test
    void doQueryWithInvalidTokenFromHeader() {
        String invalidToken = "1234";
        String expectedMessage = "Token is not valid for URL '" + BASE_PATH + QUERY_ENDPOINT + "'";

        given()
            .header("Authorization", "Bearer " + invalidToken)
            .contentType(JSON)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
            "messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage)
        );
    }

    @Test
    void doQueryWithInvalidTokenFromCookie() {
        String invalidToken = "1234";
        String expectedMessage = "Token is not valid for URL '" + BASE_PATH + QUERY_ENDPOINT + "'";

        given()
            .cookie(COOKIE, invalidToken)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void doQueryWithoutHeaderOrCookie() {
        String expectedMessage = "No authorization token provided for URL '" + BASE_PATH + QUERY_ENDPOINT + "'";

        given()
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void doQueryWithWrongAuthType() {
        String expectedMessage = "No authorization token provided for URL '" + BASE_PATH + QUERY_ENDPOINT + "'";

        given()
            .header("Authorization", "Basic " + token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void doQueryWithWrongCookieName() {
        String invalidCookie = "badCookie";
        String expectedMessage = "No authorization token provided for URL '" + BASE_PATH + QUERY_ENDPOINT + "'";

        given()
            .cookie(invalidCookie, token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void doQueryWithEmptyHeader() {
        String emptyToken = " ";
        String expectedMessage = "No authorization token provided for URL '" + BASE_PATH + QUERY_ENDPOINT + "'";

        given()
            .header("Authorization", "Bearer " + emptyToken)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void doQueryWithWrongHttpMethod() {
        String expectedMessage = "Authentication method 'POST' is not supported for URL '" +
            BASE_PATH + QUERY_ENDPOINT + "'";

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT))
        .then()
            .body(
            "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
            );
    }
    //@formatter:on
}
