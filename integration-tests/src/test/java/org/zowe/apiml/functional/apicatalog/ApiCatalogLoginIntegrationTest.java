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
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.security.SecurityUtils.readPassword;

@GeneralAuthenticationTest
class ApiCatalogLoginIntegrationTest implements TestWithStartedInstances {

    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));

    private static final String API_PREFIX = "/api/v1";
    private static final String GATEWAY_SERVICE_ID = "/gateway";
    private static final String CATALOG_SERVICE_ID = "/apicatalog";
    private static final String LOGIN_ENDPOINT = "/auth/login";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String PASSWORD = new String(readPassword(ConfigReader.environmentConfiguration().getCredentials().getPassword()));
    private static final String INVALID_USERNAME = "incorrectUser";
    private static final String INVALID_PASSWORD = "incorrectPassword";

    private static final URI LOGIN_ENDPOINT_URL_CATALOG = HttpRequestUtils.getUriFromGateway(CATALOG_SERVICE_ID + API_PREFIX + LOGIN_ENDPOINT);
    private static final String LOGIN_ENDPOINT_URL_GATEWAY = GATEWAY_SERVICE_ID + API_PREFIX + LOGIN_ENDPOINT;

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class Microservices {

        @BeforeEach
        void checkIfMicroserviceIsAvailable() {
            assumeFalse(IS_MODULITH_ENABLED);
        }

        //@formatter:off
        @Test
        void doLoginWithValidBodyLoginRequest() {
            LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD.toCharArray());

            given()
                .contentType(JSON)
                .body(loginRequest)
            .when()
                .post(LOGIN_ENDPOINT_URL_CATALOG)
            .then()
                .statusCode(is(SC_NO_CONTENT))
                .cookie(COOKIE_NAME, not(is(emptyString())))
                .extract().detailedCookie(COOKIE_NAME);
        }

        @Test
        void doLoginWithInvalidCredentialsInLoginRequest() {
            String expectedMessage = "Invalid username or password for URL '" + CATALOG_SERVICE_ID + LOGIN_ENDPOINT + "'";

            LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD.toCharArray());

            given()
                .contentType(JSON)
                .body(loginRequest)
            .when()
                .post(LOGIN_ENDPOINT_URL_CATALOG)
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
                .post(LOGIN_ENDPOINT_URL_CATALOG)
            .then()
                .statusCode(is(SC_BAD_REQUEST))
                .body(
                    "messages.find { it.messageNumber == 'ZWEAS121E' }.messageContent", equalTo(expectedMessage)
                );
        }
        //@formatter:on

    }

    @Nested
    class Modulith {

        @BeforeEach
        void checkIfModulithIsAvailable() {
            assumeTrue(IS_MODULITH_ENABLED);
        }

        //@formatter:off
        @Test
        void doLoginWithValidBodyLoginRequest() {
            LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD.toCharArray());

            given()
                .contentType(JSON)
                .body(loginRequest)
            .when()
                .post(LOGIN_ENDPOINT_URL_CATALOG)
            .then()
                .statusCode(is(308))
                .header(HttpHeaders.LOCATION, LOGIN_ENDPOINT_URL_GATEWAY);
        }

        @Test
        void doLoginWithInvalidCredentialsInLoginRequest() {
            LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD.toCharArray());

            given()
                .contentType(JSON)
                .body(loginRequest)
            .when()
                .post(LOGIN_ENDPOINT_URL_CATALOG)
            .then()
                .statusCode(is(308))
                .header(HttpHeaders.LOCATION, LOGIN_ENDPOINT_URL_GATEWAY);
        }

        @Test
        void doLoginWithoutCredentials() {
            given()
            .when()
                .post(LOGIN_ENDPOINT_URL_CATALOG)
            .then()
                .statusCode(is(308))
                .header(HttpHeaders.LOCATION, LOGIN_ENDPOINT_URL_GATEWAY);
        }
        //@formatter:on

    }

}
