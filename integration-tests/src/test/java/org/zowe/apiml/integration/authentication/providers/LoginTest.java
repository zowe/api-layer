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
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.SAFAuthTest;
import org.zowe.apiml.util.categories.zOSMFAuthTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_LOGIN;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_LOGIN_OLD_FORMAT;

/**
 * Basic set of login related tests that needs to pass against every valid authentication provider.
 */
@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
class LoginTest implements TestWithStartedInstances {

    public static final URI LOGIN_ENDPOINT_URL = HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN);
    public static final URI LOGIN_ENDPOINT_URL_OLD_FORMAT = HttpRequestUtils.getUriFromGateway(ROUTED_LOGIN_OLD_FORMAT);

    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String INVALID_USERNAME = "incorrectUser";
    private final static String INVALID_PASSWORD = "incorrectPassword";

    protected static URI[] loginUrlsSource() {
        return new URI[]{LOGIN_ENDPOINT_URL, LOGIN_ENDPOINT_URL_OLD_FORMAT};
    }

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
            @ParameterizedTest(name = "givenValidCredentialsInBody {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenValidCredentialsInBody(URI loginUrl) {
                LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword().toCharArray());

                Cookie cookie = given()
                    .contentType(JSON)
                    .body(loginRequest)
                .when()
                    .post(loginUrl)
                .then()
                    .statusCode(is(SC_NO_CONTENT))
                    // RestAssured version in use doesn't have SameSite attribute in cookie so validate using the Set-Cookie header
                    .header("Set-Cookie", containsString("SameSite=Strict"))
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
            @ParameterizedTest(name = "givenInvalidCredentialsInBody {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.LoginTest#loginUrlsSource")
            void givenInvalidCredentialsInBody(URI loginUrl) {
                String expectedMessage = "Invalid username or password for URL '" + getPath(loginUrl) + "'";

                LoginRequest loginRequest = new LoginRequest(INVALID_USERNAME, INVALID_PASSWORD.toCharArray());

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

                String headerValue = "Basic " + Base64.getEncoder().encodeToString((INVALID_USERNAME + ":" + INVALID_PASSWORD).getBytes(StandardCharsets.UTF_8));

                given()
                    .contentType(JSON)
                    .header(HttpHeaders.AUTHORIZATION, headerValue)
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

                LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword().toCharArray());

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

    @SuppressWarnings("unused")
    private static Stream<Arguments> testLoginFactCombinationsSource() {

        LoginRequest validLoginRequest = new LoginRequest(LoginTest.USERNAME, LoginTest.PASSWORD.toCharArray());
        LoginRequest incorrectUser = new LoginRequest("aaa", "aaa".toCharArray());
        URI loginNew = LOGIN_ENDPOINT_URL;
        URI loginOld = LOGIN_ENDPOINT_URL_OLD_FORMAT;

        return Stream.of(
            //URI loginUrl, RestAssuredConfig config, LoginRequest loginRequest, String user, String pw, HttpStatus rc, String loggedUser

            Arguments.of("Login with body no cert", loginNew, SslContext.tlsWithoutCert, validLoginRequest, null, null, HttpStatus.NO_CONTENT, LoginTest.USERNAME),
            Arguments.of("Login with body no cert", loginOld, SslContext.tlsWithoutCert, validLoginRequest, null, null, HttpStatus.NO_CONTENT, LoginTest.USERNAME),

            Arguments.of("Login with basic and body (basic has precedence)", loginNew, SslContext.tlsWithoutCert, validLoginRequest, "aaaa", "aaaa", HttpStatus.UNAUTHORIZED, null),
            Arguments.of("Login with basic and body (basic has precedence)", loginOld, SslContext.tlsWithoutCert, validLoginRequest, "aaaa", "aaaa", HttpStatus.UNAUTHORIZED, null),

            Arguments.of("Login with trusted cert and body (body or basic has precedence)", loginNew, SslContext.clientCertValid, validLoginRequest, null, null, HttpStatus.NO_CONTENT, LoginTest.USERNAME),
            Arguments.of("Login with trusted cert and body (body or basic has precedence)", loginOld, SslContext.clientCertValid, validLoginRequest, null, null, HttpStatus.NO_CONTENT, LoginTest.USERNAME),

            Arguments.of("Login with aml cert (aml cert filtered out)", loginNew, SslContext.clientCertApiml, null, null, null, HttpStatus.BAD_REQUEST, null),
            Arguments.of("Login with aml cert (aml cert filtered out)", loginOld, SslContext.clientCertApiml, null, null, null, HttpStatus.BAD_REQUEST, null),

            Arguments.of("Login with trusted cert", loginNew, SslContext.clientCertValid, null, null, null, HttpStatus.NO_CONTENT, "PROD001"),
            Arguments.of("Login with trusted cert", loginOld, SslContext.clientCertValid, null, null, null, HttpStatus.NO_CONTENT, "PROD001"),

            Arguments.of("Login with trusted cert and Basic (body or basic has precedence)", loginNew, SslContext.clientCertValid, null, LoginTest.USERNAME, LoginTest.PASSWORD, HttpStatus.NO_CONTENT, LoginTest.USERNAME),
            Arguments.of("Login with trusted cert and Basic (body or basic has precedence)", loginOld, SslContext.clientCertValid, null, LoginTest.USERNAME, LoginTest.PASSWORD, HttpStatus.NO_CONTENT, LoginTest.USERNAME)
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
