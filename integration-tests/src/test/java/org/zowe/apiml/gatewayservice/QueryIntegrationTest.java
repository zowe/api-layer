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
import io.restassured.response.ValidatableResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.zowe.apiml.util.config.ConfigReader;

import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

@RunWith(value = Parameterized.class)
public class QueryIntegrationTest {

    private static final String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private static final String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private static final int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private static final String PATH_PREFIX = "/api/v1/gateway";
    private static final String QUERY_ENDPOINT = "/auth/query";
    private static final String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String COOKIE = "apimlAuthenticationToken";

    private String token;

    private String basePath;

    public QueryIntegrationTest(String basePath) {
        this.basePath = basePath;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {""},
            {PATH_PREFIX}
        });
    }

    private boolean rejectedOnZull() {
        return !basePath.isEmpty();
    }

    @Before
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.basePath = basePath;
        RestAssured.useRelaxedHTTPSValidation();

        token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
    }

    //@formatter:off
    @Test
    public void doQueryWithValidTokenFromHeader() {
        given()
             .header("Authorization", "Bearer " + token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body("userId", equalTo(USERNAME));
    }

    @Test
    public void doQueryWithValidTokenFromCookie() {
        given()
            .cookie(COOKIE, token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_OK))
            .body("userId", equalTo(USERNAME));
    }

    @Test
    public void doQueryWithInvalidTokenFromHeader() {
        String invalidToken = "1234";
        String expectedMessage = "Token is not valid for URL '" + QUERY_ENDPOINT + "'";

        ValidatableResponse vr = given()
            .header("Authorization", "Bearer " + invalidToken)
            .contentType(JSON)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));

        if (!rejectedOnZull()) {
            vr.body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
        }
    }

    @Test
    public void doQueryWithInvalidTokenFromCookie() {
        String invalidToken = "1234";
        String expectedMessage = "Token is not valid for URL '" + basePath + QUERY_ENDPOINT + "'";

        given()
            .cookie(COOKIE, invalidToken)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithoutHeaderOrCookie() {
        String expectedMessage = "No authorization token provided for URL '" + QUERY_ENDPOINT + "'";

        given()
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithWrongAuthType() {
        String expectedMessage = "No authorization token provided for URL '" + QUERY_ENDPOINT + "'";

        ValidatableResponse response = given()
            .header("Authorization", "Basic " + token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));

        // if call is via ZUUL, it is stop before endpoint, without any message
        if (basePath.isEmpty()) {
            response.body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
        }
    }

    @Test
    public void doQueryWithWrongCookieName() {
        String invalidCookie = "badCookie";
        String expectedMessage = "No authorization token provided for URL '" + QUERY_ENDPOINT + "'";

        given()
            .cookie(invalidCookie, token)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithEmptyHeader() {
        String emptyToken = " ";
        String expectedMessage = "No authorization token provided for URL '" + QUERY_ENDPOINT + "'";

        given()
            .header("Authorization", "Bearer " + emptyToken)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doQueryWithWrongHttpMethod() {
        String expectedMessage = "Authentication method 'POST' is not supported for URL '" + QUERY_ENDPOINT + "'";

        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, QUERY_ENDPOINT))
        .then()
            .body(
            "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
            );
    }
    //@formatter:on
}
