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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.config.ConfigReader;

import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(value = Parameterized.class)
public class LoginIntegrationTest {

    private static final String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private static final String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost();
    private static final int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private static final String PATH_PREFIX = "/api/v1/gateway";
    private static final String LOGIN_ENDPOINT = "/auth/login";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private static final String INVALID_USERNAME = "incorrectUser";
    private static final String INVALID_PASSWORD = "incorrectPassword";

    private String basePath;

    public LoginIntegrationTest(String basePath) {
        this.basePath = basePath;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {""},
            {PATH_PREFIX}
        });
    }

    @Before
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    public void doLoginWithValidBodyLoginRequest() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertTrue(cookie.isHttpOnly());
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = Jwts.parser()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();

        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(USERNAME));
    }

    @Test
    public void doLoginWithValidHeader() {
        String token = given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
            .contentType(JSON)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);

        int i = token.lastIndexOf('.');
        String untrustedJwtString = token.substring(0, i + 1);
        Claims claims = Jwts.parser()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();

        assertThat(claims.getId(), not(isEmptyString()));
        assertThat(claims.getSubject(), is(USERNAME));
    }

    @Test
    public void doLoginWithInvalidCredentialsInLoginRequest() {
        String expectedMessage = "Invalid username or password for URL '" + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doLoginWithInvalidCredentialsInHeader() {
        String expectedMessage = "Invalid username or password for URL '" + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doLoginWithoutCredentials() {
        String expectedMessage = "Authorization header is missing, or request body is missing or invalid for URL '" +
            LOGIN_ENDPOINT + "'";

        given()
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doLoginWithInvalidLoginRequest() {
        String expectedMessage = "Authorization header is missing, or request body is missing or invalid for URL '" +
            LOGIN_ENDPOINT + "'";

        JSONObject loginRequest = new JSONObject()
            .put("user",USERNAME)
            .put("pass",PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest.toString())
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doLoginWithWrongHttpMethod() {
        String expectedMessage = "Authentication method 'GET' is not supported for URL '" +
            LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, basePath, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
            );
    }
    //@formatter:on
}
