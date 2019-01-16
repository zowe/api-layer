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

import com.ca.mfaas.security.login.LoginRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LoginIntegrationTest {
    private final static String LOGIN_ENDPOINT = "/auth/login";
    private final static String TOKEN = "apimlAuthenticationToken";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private final static String USERNAME = "melva02";
    private final static String PASSWORD = "ache3goo";
    private final static String INVALID_USERNAME = "intstusr";
    private final static String INVALID_PASSWORD = "someps33";

    @Before
    public void setUp() {
        RestAssured.port = 10010;
    }

    @Test
    public void doLoginWithValidBodyLoginRequest() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        String token = given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(LOGIN_ENDPOINT)
        .then()
            .statusCode(is(SC_OK))
            .cookie(TOKEN, not(isEmptyString()))
            .body(
                TOKEN, not(isEmptyString())
            )
            .extract().cookie(COOKIE_NAME);

        Claims claims = Jwts.parser()
            .setSigningKey("secret")
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(USERNAME));
        assertThat(claims.getIssuer(), is("gateway"));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertTrue(ttl > 0);
    }

    @Test
    public void doLoginWithValidHeaderLoginRequest() {
        String token = given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
            .contentType(JSON)
            .when()
            .post(LOGIN_ENDPOINT)
            .then()
            .statusCode(is(SC_OK))
            .cookie(TOKEN, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);

        String username = "apimtst";
        Claims claims = Jwts.parser()
            .setSigningKey("secret")
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(username));
        assertThat(claims.getIssuer(), is("gateway"));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertTrue(ttl > 0);
    }

    @Test
    public void doLoginWithInvalidCredentialsInHeaderLoginRequest() {
        String expectedMessage = "Username or password is incorrect";
        given()
            .auth().preemptive().basic(INVALID_USERNAME, INVALID_PASSWORD)
            .contentType(JSON)
            .when()
            .post(LOGIN_ENDPOINT)
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_UNAUTHORIZED),
                "error.code", equalTo("SEC0002")
            );
    }

    @Test
    public void doLoginWithBadHeaderLoginRequest() {
        String expectedMessage = "Login object has wrong format";
        given()
            .header("Authorization", "Basic 12345")
            .contentType(JSON)
            .when()
            .post(LOGIN_ENDPOINT)
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_BAD_REQUEST),
                "error.code", equalTo("SEC0001")
            );
    }

    @Test
    public void doLoginWithInvalidLoginRequest() {
        String expectedMessage = "Login object has wrong format";
        JSONObject loginRequest = new JSONObject()
            .put("user","apimltst")
            .put("pass","test");

        given()
            .contentType(JSON)
            .body(loginRequest.toString())
        .when()
            .post(LOGIN_ENDPOINT)
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_BAD_REQUEST),
                "error.code", equalTo("SEC0001")
            );
    }

    @Test
    public void doLoginWithInvalidCredentials() {
        String expectedMessage = "Username or password is incorrect";

        GatewayLoginRequest loginRequest = new GatewayLoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(LOGIN_ENDPOINT)
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_UNAUTHORIZED),
                "error.code", equalTo("SEC0002")
            );
    }

    @Test
    public void doLoginWithWrongHttpMethod() {
        String expectedMessage = "Request method 'PUT' not supported";

        GatewayLoginRequest loginRequest = new GatewayLoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .put(LOGIN_ENDPOINT)
            .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body(
                "error.message", equalTo(expectedMessage),
                "error.status", equalTo(SC_METHOD_NOT_ALLOWED),
                "error.code", equalTo("SEC0003")
            );
    }
}
