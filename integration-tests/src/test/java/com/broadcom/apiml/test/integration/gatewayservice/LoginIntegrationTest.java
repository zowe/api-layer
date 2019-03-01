/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.test.integration.gatewayservice;

import com.broadcom.apiml.service.security.login.LoginRequest;
import com.broadcom.apiml.test.integration.utils.config.ConfigReader;
import com.broadcom.apiml.test.integration.utils.config.GatewayServiceConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
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
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private final static String USERNAME = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getApiCatalogServiceConfiguration().getPassword();
    private final static String INVALID_USERNAME = "intstusr";
    private final static String INVALID_PASSWORD = "someps33";

    private GatewayServiceConfiguration serviceConfiguration;
    private String scheme;
    private String host;
    private int port;
    private String basePath;

    @Before
    public void setUp() {
        serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = serviceConfiguration.getScheme();
        host = serviceConfiguration.getHost();
        port = serviceConfiguration.getPort();
        basePath = "/api/v1/gateway";

        RestAssured.port = serviceConfiguration.getPort();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void doLoginWithValidBodyLoginRequest() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        String token = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);

        int i = token.lastIndexOf('.');
        String untrustedJwtString = token.substring(0, i + 1);
        Claims claims = Jwts.parser()
            .parseClaimsJwt(untrustedJwtString)
            .getBody();

        assertThat(claims.getSubject(), is(USERNAME));
        assertThat(claims.getId(), not(isEmptyString()));
        assertTrue(claims.get("ltpa").toString().contains("LtpaToken2="));
    }

    @Ignore
    @Test
    public void doLoginWithValidHeaderLoginRequest() {
        String token = given()
            .auth().preemptive().basic(USERNAME, PASSWORD)
            .contentType(JSON)
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_OK))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().cookie(COOKIE_NAME);
    }

    @Test
    public void doLoginWithInvalidCredentialsInHeaderLoginRequest() {
        String expectedMessage = "Authentication problem: 'Login object has wrong format' for URL '/apicatalog/auth/login'";
        given()
            .auth().preemptive().basic(INVALID_USERNAME, INVALID_PASSWORD)
            .contentType(JSON)
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0002' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Ignore
    @Test
    public void doLoginWithBadHeaderLoginRequest() {
        String expectedMessage = "Login object has wrong format";
        given()
            .header("Authorization", "Basic 12345")
            .contentType(JSON)
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'SEC0002' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Ignore
    @Test
    public void doLoginWithInvalidLoginRequest() {
        String expectedMessage = "Login object has wrong format";
        JSONObject loginRequest = new JSONObject()
            .put("user", "apimltst")
            .put("pass", "test");

        given()
            .contentType(JSON)
            .body(loginRequest.toString())
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'SEC0002' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    public void doLoginWithInvalidCredentials() {
        String expectedMessage = "Authentication problem: 'Username or password are invalid.' for URL '/apicatalog/auth/login'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'SEC0005' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Ignore
    @Test
    public void doLoginWithWrongHttpMethod() {
        String expectedMessage = "Request method 'PUT' not supported";

        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .put(String.format("%s://%s:%d%s%s", scheme, host, port, basePath, LOGIN_ENDPOINT))
            .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body(
                "messages.find { it.messageNumber == 'SEC0002' }.messageContent", equalTo(expectedMessage)
            );
    }
}
