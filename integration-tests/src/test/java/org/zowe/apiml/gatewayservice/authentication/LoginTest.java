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
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;

import java.net.URI;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

abstract class LoginTest {
    protected final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    protected final static String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    protected final static String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    protected final static String BASE_PATH = "/gateway/api/v1";
    protected final static String BASE_PATH_OLD_FORMAT = "/api/v1/gateway";
    protected final static String LOGIN_ENDPOINT = "/auth/login";
    public static final String LOGIN_ENDPOINT_URL = String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT);
    public static final String LOGIN_ENDPOINT_URL_OLD_FORMAT = String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH_OLD_FORMAT, LOGIN_ENDPOINT);

    protected static String[] loginUrlsSource() {
        return new String[]{LOGIN_ENDPOINT_URL, LOGIN_ENDPOINT_URL_OLD_FORMAT};
    }

    protected final static String COOKIE_NAME = "apimlAuthenticationToken";

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    protected String getUsername() {
        return USERNAME;
    }

    protected String getPassword() {
        return PASSWORD;
    }

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication();
    }


    @BeforeEach
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenValidCredentialsInBody_whenUserAuthenticates_thenTheValidTokenIsProduced(String loginUrl) {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(loginUrl)
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertValidAuthToken(cookie);
    }

    protected void assertValidAuthToken(Cookie cookie) {
        assertValidAuthToken(cookie, Optional.empty());
    }

    protected void assertValidAuthToken(Cookie cookie, Optional<String> username) {
        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims, username);
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenValidCredentialsInHeader_whenUserAuthenticates_thenTheValidTokenIsProduced(String loginUrl) {
        String token = given()
            .auth().preemptive().basic(getUsername(), getPassword())
            .contentType(JSON)
            .when()
            .post(loginUrl)
            .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);

        int i = token.lastIndexOf('.');
        String untrustedJwtString = token.substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims);
    }

    protected void assertThatTokenIsValid(Claims claims) {
        assertThatTokenIsValid(claims, Optional.empty());
    }

    protected void assertThatTokenIsValid(Claims claims, Optional<String> username) {
        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(username.orElseGet(this::getUsername)));
    }

    protected Claims parseJwtString(String untrustedJwtString) {
        return Jwts.parserBuilder().build()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenInvalidCredentialsInBody_whenUserAuthenticates_thenUnauthorizedIsReturned(String loginUrl) {
        String loginPath = loginUrl.substring(StringUtils.ordinalIndexOf(loginUrl, "/", 3));
        String expectedMessage = "Invalid username or password for URL '" + loginPath + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(loginUrl)
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenInvalidCredentialsInHeader_whenUserAuthenticates_thenUnauthorizedIsReturned(String loginUrl) {
        String loginPath = loginUrl.substring(StringUtils.ordinalIndexOf(loginUrl, "/", 3));
        String expectedMessage = "Invalid username or password for URL '" + loginPath + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        ValidatableResponse response = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(loginUrl)
            .then();
        response.statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenCredentialsInTheWrongJsonFormat_whenUserAuthenticates_then400IsReturned(String loginUrl) {
        String loginPath = loginUrl.substring(StringUtils.ordinalIndexOf(loginUrl, "/", 3));
        String expectedMessage = "Authorization header is missing, or the request body is missing or invalid for URL '" + loginPath + "'";

        JSONObject loginRequest = new JSONObject()
            .put("user", getUsername())
            .put("pass", getPassword());

        given()
            .contentType(JSON)
            .body(loginRequest.toString())
            .when()
            .post(loginUrl)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenValidCredentialsInJsonBody_whenUserAuthenticatesViaGetMethod_then405IsReturned(String loginUrl) {
        String loginPath = loginUrl.substring(StringUtils.ordinalIndexOf(loginUrl, "/", 3));
        String expectedMessage = "Authentication method 'GET' is not supported for URL '" + loginPath + "'";

        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .get(loginUrl)
            .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
            );
    }

    protected String authenticateAndVerify(LoginRequest loginRequest, String url) {
        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(url)
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

        return cookie.getValue();
    }

    @ParameterizedTest
    @MethodSource("loginUrlsSource")
    void givenApimlsCert_whenAuth_thenUnauthorized(String loginUrl) throws Exception {
        given().config(SslContext.clientCertApiml)
            .post(new URI(loginUrl))
            .then()
            .statusCode(is(SC_BAD_REQUEST));
    }
    //@formatter:on
}
