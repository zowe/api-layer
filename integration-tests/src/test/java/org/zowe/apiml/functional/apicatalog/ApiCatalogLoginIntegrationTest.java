/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.apicatalog;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@GeneralAuthenticationTest
class ApiCatalogLoginIntegrationTest implements TestWithStartedInstances {
    private final static String CATALOG_PREFIX = "/api/v1";
    private final static String CATALOG_SERVICE_ID = "/apicatalog";
    private final static String LOGIN_ENDPOINT = "/auth/login";
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    private final static URI LOGIN_ENDPOINT_URL = HttpRequestUtils.getUriFromGateway(CATALOG_SERVICE_ID + CATALOG_PREFIX + LOGIN_ENDPOINT);

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    void doLoginWithValidBodyLoginRequest() {
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(LOGIN_ENDPOINT_URL)
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);
    }

    @Test
    void doLoginWithInvalidCredentialsInLoginRequest() {
        String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID + LOGIN_ENDPOINT + "'";

        LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

        given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(LOGIN_ENDPOINT_URL)
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body(
                "messages.find { it.messageNumber == 'ZWEAS120E' }.messageContent", equalTo(expectedMessage)
            );
    }

    @Test
    void doLoginWithoutCredentials() {
        String expectedMessage = "Authorization header is missing, or the request body is missing or invalid for URL '" +
            CATALOG_SERVICE_ID + LOGIN_ENDPOINT + "'";

        given()
        .when()
            .post(LOGIN_ENDPOINT_URL)
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body(
                "messages.find { it.messageNumber == 'ZWEAS121E' }.messageContent", equalTo(expectedMessage)
            );
    }
    //@formatter:on
}
