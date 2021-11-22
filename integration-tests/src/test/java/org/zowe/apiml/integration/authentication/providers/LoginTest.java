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
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

/**
 * Basic set of login related tests that needs to pass against every valid authentication provider.
 */
@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
class LoginTest implements TestWithStartedInstances {
    public static final URI LOGIN_ENDPOINT_URL = HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN);
    
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    public String getUsername() {
        return USERNAME;
    }

    public String getPassword() {
        return PASSWORD;
    }

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenUserAuthenticates {
        @Nested
        class ReturnsValidToken {
            @Test
            void givenValidCredentialsInBody() {
                LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

                Cookie cookie = given()
                    .contentType(JSON)
                    .body(loginRequest)
                .when()
                    .post(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_NO_CONTENT))
                    // RestAssured version in use doesn't have SameSite attribute in cookie so validate using the Set-Cookie header
                    .header("Set-Cookie", containsString("SameSite=Strict"))
                    .cookie(COOKIE_NAME, not(isEmptyString()))
                    .extract().detailedCookie(COOKIE_NAME);

                assertValidAuthToken(cookie);
            }

            @Test
            void givenValidCredentialsInHeader() {
                String token = given()
                    .auth().preemptive().basic(getUsername(), getPassword())
                    .contentType(JSON)
                .when()
                    .post(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_NO_CONTENT))
                    // RestAssured version in use doesn't have SameSite attribute in cookie so validate using the Set-Cookie header
                    .header("Set-Cookie", containsString("SameSite=Strict"))
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
            @Test
            void givenInvalidCredentialsInBody() {
                String expectedMessage = "Invalid username or password for URL '" + getPath(LOGIN_ENDPOINT_URL) + "'";

                LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

                given()
                    .contentType(JSON)
                    .body(loginRequest)
                .when()
                    .post(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @Test
            void givenInvalidCredentialsInHeader() {
                String expectedMessage = "Invalid username or password for URL '" + getPath(LOGIN_ENDPOINT_URL) + "'";

                LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD);

                given()
                    .contentType(JSON)
                    .body(loginRequest)
                .when()
                    .post(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG120E' }.messageContent", equalTo(expectedMessage)
                    );
            }
        }

        @Nested
        class ReturnsBadRequest {
            @Test
            void givenCredentialsInTheWrongJsonFormat() {
                String expectedMessage = "Authorization header is missing, or the request body is missing or invalid for URL '" + getPath(LOGIN_ENDPOINT_URL) + "'";

                JSONObject loginRequest = new JSONObject()
                    .put("user", getUsername())
                    .put("pass", getPassword());

                given()
                    .contentType(JSON)
                    .body(loginRequest.toString())
                .when()
                    .post(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_BAD_REQUEST))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG121E' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @Test
            void givenApimlsCert() {
                given()
                    .config(SslContext.clientCertApiml)
                .when()
                    .post(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_BAD_REQUEST));
            }

        }
    }

    @Nested
    class WhenUserAuthenticatesViaGetMethod {
        @Nested
        class ReturnMethodNotAllowed {

            @Test
            void givenValidCredentialsInJsonBody() {
                String expectedMessage = "Authentication method 'GET' is not supported for URL '" + getPath(LOGIN_ENDPOINT_URL) + "'";

                LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

                given()
                    .contentType(JSON)
                    .body(loginRequest)
                .when()
                    .get(LOGIN_ENDPOINT_URL)
                .then()
                    .statusCode(is(SC_METHOD_NOT_ALLOWED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
                    );
            }
        }
    }
    //@formatter:on

    private static Stream<Arguments> testLoginFactCombinationsSource() {

        LoginRequest validLoginRequest = new LoginRequest(LoginTest.USERNAME, LoginTest.PASSWORD);
        URI login = LOGIN_ENDPOINT_URL;

        return Stream.of(
            //URI loginUrl, RestAssuredConfig config, LoginRequest loginRequest, String user, String pw, HttpStatus rc, String loggedUser

            Arguments.of("Login with body no cert", login, SslContext.tlsWithoutCert, validLoginRequest, null, null, HttpStatus.NO_CONTENT, LoginTest.USERNAME),

            Arguments.of("Login with basic and body (basic has precedence)", login, SslContext.tlsWithoutCert, validLoginRequest, "aaaa", "aaaa", HttpStatus.UNAUTHORIZED, null),

            Arguments.of("Login with trusted cert and body (body or basic has precedence)", login, SslContext.clientCertValid, validLoginRequest, null, null, HttpStatus.NO_CONTENT, LoginTest.USERNAME),

            Arguments.of("Login with aml cert (aml cert filtered out)", login, SslContext.clientCertApiml, null, null, null, HttpStatus.BAD_REQUEST, null),

            Arguments.of("Login with trusted cert", login, SslContext.clientCertValid, null, null, null, HttpStatus.NO_CONTENT, "APIMTST"),

            Arguments.of("Login with trusted cert and Basic (body or basic has precedence)", login, SslContext.clientCertValid, null, LoginTest.USERNAME, LoginTest.PASSWORD, HttpStatus.NO_CONTENT, LoginTest.USERNAME)
            );
    }

    @Nested
    class GivenCombinationsOfAuthenticationFacts {

        @ParameterizedTest(name = "{0} on {1}")
        @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#testLoginFactCombinationsSource")
        void loginEndpointActsAsExpected(String testName, URI loginUrl, RestAssuredConfig config, LoginRequest loginRequest, String user, String pw, HttpStatus rc, String loggedUser) {

            RequestSpecification r = given().config(config);
            if (user != null) {
                r.auth().preemptive().basic(user, pw);
            }
            if (loginRequest != null) {
                r.contentType(JSON).body(loginRequest);
            }

            Response response = r.when()
                .post(loginUrl);

            response.then()
            .statusCode(is(rc.value()));

            if (loggedUser != null) {
                Cookie cookie = response.detailedCookie(COOKIE_NAME);
                assertValidAuthToken(cookie, Optional.of(loggedUser));
            }

        }
    }


    private String getPath(URI loginUrl) {
        String urlPath = loginUrl.getPath();

        return urlPath.substring(StringUtils.ordinalIndexOf(urlPath, "/", 1));
    }
}
