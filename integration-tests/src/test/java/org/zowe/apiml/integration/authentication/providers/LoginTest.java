/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication.providers;

import io.jsonwebtoken.Claims;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.*;

/**
 * Basic set of login related tests that needs to pass against every valid authentication provider.
 */
@GeneralAuthenticationTest
class LoginTest implements TestWithStartedInstances {
    protected final static String BASE_PATH = "/gateway/api/v1";
    protected final static String BASE_PATH_OLD_FORMAT = "/api/v1/gateway";
    protected final static String LOGIN_ENDPOINT = "/auth/login";

    public static final URI LOGIN_ENDPOINT_URL = HttpRequestUtils.getUriFromGateway(BASE_PATH + LOGIN_ENDPOINT);
    public static final URI LOGIN_ENDPOINT_URL_OLD_FORMAT = HttpRequestUtils.getUriFromGateway(BASE_PATH_OLD_FORMAT + LOGIN_ENDPOINT);

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    protected static URI[] loginUrlsSource() {
        return new URI[]{LOGIN_ENDPOINT_URL, LOGIN_ENDPOINT_URL_OLD_FORMAT};
    }

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
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenUserAuthenticates {
        @Nested
        class ReturnsValidToken {
            @ParameterizedTest(name = "givenValidCredentialsInBody {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenValidCredentialsInBody(URI loginUrl) {
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

            @ParameterizedTest(name = "givenValidCredentialsInHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenValidCredentialsInHeader(URI loginUrl) {
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
        }

        @Nested
        class ReturnsUnauthorized {
            @ParameterizedTest(name = "givenInvalidCredentialsInBody {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenInvalidCredentialsInBody(URI loginUrl) {
                String expectedMessage = "Invalid username or password for URL '" + getPath(loginUrl) + "'";

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

            @ParameterizedTest(name = "givenInvalidCredentialsInHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenInvalidCredentialsInHeader(URI loginUrl) {
                String expectedMessage = "Invalid username or password for URL '" + getPath(loginUrl) + "'";

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
        }

        @Nested
        class ReturnsBadRequest {
            @ParameterizedTest(name = "givenCredentialsInTheWrongJsonFormat {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenCredentialsInTheWrongJsonFormat(URI loginUrl) {
                String expectedMessage = "Authorization header is missing, or the request body is missing or invalid for URL '" + getPath(loginUrl) + "'";

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

            @ParameterizedTest(name = "givenApimlsCert {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenApimlsCert(URI loginUrl) {
                given()
                    .config(SslContext.clientCertApiml)
                .when()
                    .post(loginUrl)
                .then()
                    .statusCode(is(SC_BAD_REQUEST));
            }
        }
    }

    @Nested
    class WhenUserAuthenticatesViaGetMethod {
        @Nested
        class ReturnMethodNotAllowed {

            @ParameterizedTest(name = "givenValidCredentialsInJsonBody {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenValidCredentialsInJsonBody(URI loginUrl) {
                String expectedMessage = "Authentication method 'GET' is not supported for URL '" + getPath(loginUrl) + "'";

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
        }
    }
    //@formatter:on

    private String getPath(URI loginUrl) {
        String urlPath = loginUrl.getPath();

        return urlPath.substring(StringUtils.ordinalIndexOf(urlPath, "/", 1));
    }
}
