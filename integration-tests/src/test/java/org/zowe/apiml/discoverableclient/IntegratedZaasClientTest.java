/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

/**
 * Purpose of this test is verify correct behavior of Zaas client
 * as a part of application running on mainframe
 */
@TestsNotMeantForZowe
class IntegratedZaasClientTest {
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private final static String LOGIN = "/login";
    private final static String LOGOUT = "/logout";

    private final static URI ZAAS_CLIENT_URI = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/zaasClient");
    private final static URI ZAAS_CLIENT_URI_OLD_FORMAT = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/zaasClient");

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    /**
     * This method is testing a communication between discoverable client application
     * using Zaas client and gateway service on mainframe. Main goal is to test correct
     * configuration of SSL, specifically SAF keyring support.
     */
    @Test
    void loginWithValidCredentials() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(ZAAS_CLIENT_URI + LOGIN)
            .then()
            .statusCode(is(SC_OK))
            .body(not(isEmptyString()));
    }

    /**
     * This method is testing a communication between discoverable client application
     * using Zaas client and gateway service on mainframe. Main goal is to test correct
     * configuration of SSL, specifically SAF keyring support.
     */
    @Test
    void invalidCredentials() {
        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(ZAAS_CLIENT_URI + LOGIN)
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(is("Invalid username or password"));
    }

    @Test
    void loginWithValidCredentials_OldPathFormat() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(ZAAS_CLIENT_URI_OLD_FORMAT + LOGIN)
            .then()
            .statusCode(is(SC_OK))
            .body(not(isEmptyString()));
    }

    @Test
    void invalidCredentials_OldPathFormat() {
        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(ZAAS_CLIENT_URI_OLD_FORMAT + LOGIN)
            .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(is("Invalid username or password"));
    }

    @Test
    void givenValidToken_whenCallingLogout_thenSuccess() {
        String token = "validToken";

        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, generateToken())
            .when()
            .post(ZAAS_CLIENT_URI_OLD_FORMAT + LOGOUT)
            .then()
            .statusCode(is(SC_NO_CONTENT));
    }

    @Test
    void givenInvalidToken_whenCallingLogout_thenFail() {

        given()
            .contentType(JSON)
            .when()
            .post(ZAAS_CLIENT_URI_OLD_FORMAT + LOGOUT)
            .then()
            .statusCode(is(SC_BAD_REQUEST));
    }

    private String generateToken() {
        return SecurityUtils.gatewayToken();
    }
}
