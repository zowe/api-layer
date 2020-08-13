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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.config.ConfigReader;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

public class Login {
    protected final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    protected final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    protected final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    protected final static String BASE_PATH = "/api/v1/gateway";
    protected static String authenticationEndpointPath = String.format("%s://%s:%d%s/authentication", SCHEME, HOST, PORT, BASE_PATH);
    protected static String currentProvider;

    private final static String LOGIN_ENDPOINT = "/auth/login";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    protected static String loadCurrentProvider() {
        return given()
        .when()
            .get(authenticationEndpointPath)
        .then()
            .statusCode(is(SC_OK))
            .extract().body().jsonPath().getString("provider");
    }

    protected static void switchProvider(String provider) {
        given()
            .contentType(JSON)
            .body("{\"provider\": \"" + provider + "\"}")
        .when()
            .post(authenticationEndpointPath)
        .then()
            .statusCode(is(SC_NO_CONTENT));
    }

    @AfterAll
    static void switchToOriginalProvider() {
        switchProvider(currentProvider);
    }

    @BeforeEach
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    void givenValidCredentialsInBody_whenUserAuthenticates_thenTheValidTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims);
    }

    @Test
    void givenValidCredentialsInHeader_whenUserAuthenticates_thenTheValidTokenIsProduced() {
        String token = given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
            .contentType(JSON)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);

        int i = token.lastIndexOf('.');
        String untrustedJwtString = token.substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims);
    }

    private void assertThatTokenIsValid(Claims claims) {
        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(USERNAME));
    }

    private Claims parseJwtString(String untrustedJwtString) {
        return Jwts.parserBuilder().build()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();
    }

    @Test
    void givenInvalidCredentialsInBody_whenUserAuthenticates_thenUnauthorizedIsReturned() {
        String expectedMessage = "Invalid username or password for URL '" + BASE_PATH + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenInvalidCredentialsInHeader_whenUserAuthenticates_thenUnauthorizedIsReturned() {
        String expectedMessage = "Invalid username or password for URL '" + BASE_PATH + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenNoCredentials_whenUserAuthenticates_then400IsReturned() {
        String expectedMessage = "Authorization header is missing, or request body is missing or invalid for URL '" +
            BASE_PATH + LOGIN_ENDPOINT + "'";

        given()
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenCredentialsInTheWrongJsonFormat_whenUserAuthenticates_then400IsReturned() {
        String expectedMessage = "Authorization header is missing, or request body is missing or invalid for URL '" +
            BASE_PATH + LOGIN_ENDPOINT + "'";

        JSONObject loginRequest = new JSONObject()
            .put("user",USERNAME)
            .put("pass",PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest.toString())
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void givenValidCredentialsInJsonBody_whenUserAuthenticatesViaGetMethod_then405IsReturned() {
        String expectedMessage = "Authentication method 'GET' is not supported for URL '" +
            BASE_PATH + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
            );
    }
    //@formatter:on
}
