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

import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.SAFAuthTest;
import org.zowe.apiml.util.categories.zOSMFAuthTest;
import org.zowe.apiml.util.config.ConfigReader;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

/**
 * Verify that querying of the token works properly. The tests needs to pass against every valid authentication provider.
 */
@GeneralAuthenticationTest
@SAFAuthTest
@zOSMFAuthTest
@TestInstance(Lifecycle.PER_CLASS)
class QueryTest implements TestWithStartedInstances {
    private static final String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getScheme();
    private static final String HOST = StringUtils.isBlank(ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getDvipaHost()) ? ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost() : ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getDvipaHost();
    private static final int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private static final String BASE_PATH = "/gateway/api/v1";
    private static final String QUERY_ENDPOINT = "/auth/query";
    private static final String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String COOKIE = "apimlAuthenticationToken";

    public static final String QUERY_ENDPOINT_URL = String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, QUERY_ENDPOINT);

    private String validToken;

    static String[] queryUrlsSource() {
        return new String[]{QUERY_ENDPOINT_URL};
    }

    @BeforeAll
    void init() {
        this.validToken = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
        RestAssured.port = PORT;
        RestAssured.basePath = BASE_PATH;
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenQueryingToken {
        @Nested
        class ReturnInfo {
            //@formatter:off
            @ParameterizedTest(name = "givenValidTokenInHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenValidTokenInHeader(String queryUrl) {
                given()
                    .header("Authorization", "Bearer " + validToken)
                .when()
                    .get(queryUrl)
                .then()
                    .statusCode(is(SC_OK))
                    .body("userId", equalTo(USERNAME));
            }

            @ParameterizedTest(name = "givenValidTokenInCookie {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenValidTokenInCookie(String queryUrl) {
                given()
                    .cookie(COOKIE, validToken)
                .when()
                    .get(queryUrl)
                .then()
                    .statusCode(is(SC_OK))
                    .body("userId", equalTo(USERNAME));
            }
        }

        @Nested
        class ReturnUnauthorized {
            @ParameterizedTest(name = "givenInvalidTokenInBearerHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenInvalidTokenInBearerHeader(String queryUrl) {
                String invalidToken = "1234";
                String expectedMessage = "The request has not been applied because it lacks valid authentication credentials.";

                given()
                    .header("Authorization", "Bearer " + invalidToken)
                    .contentType(JSON)
                .when()
                    .get(queryUrl)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAO402E' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @ParameterizedTest(name = "givenInvalidTokenInCookie {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenInvalidTokenInCookie(String queryUrl) {
                String invalidToken = "1234";
                String expectedMessage = "The request has not been applied because it lacks valid authentication credentials.";

                given()
                    .cookie(COOKIE, invalidToken)
                .when()
                    .get(queryUrl)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAO402E' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @ParameterizedTest(name = "givenNoToken {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenNoToken(String queryUrl) {
                String queryPath = queryUrl.substring(StringUtils.ordinalIndexOf(queryUrl,"/",3)).replace("/gateway/", "/zaas/");
                String expectedMessage = "No authorization token provided for URL '" + queryPath + "'";

                given()
                .when()
                    .get(queryUrl)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
                    );
            }

            @ParameterizedTest(name = "givenValidTokenInWrongCookie {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenValidTokenInWrongCookie(String queryUrl) {
                String invalidCookie = "badCookie";
                String queryPath = queryUrl.substring(StringUtils.ordinalIndexOf(queryUrl,"/",3)).replace("/gateway/", "/zaas/");
                String expectedMessage = "No authorization token provided for URL '" + queryPath + "'";

                given()
                    .cookie(invalidCookie, validToken)
                .when()
                    .get(queryUrl)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage)
                    );
            }
        }
    }

    @Nested
    class WhenUserQueriesViaPostMethod {
        @Nested
        class ReturnMethodNotAllowed {
            @ParameterizedTest(name = "givenValidCredentials {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.authentication.providers.QueryTest#queryUrlsSource")
            void givenValidToken(String queryUrl) {
                String queryPath = queryUrl.substring(StringUtils.ordinalIndexOf(queryUrl,"/",3)).replace("/gateway/", "/zaas/");
                String expectedMessage = "Authentication method 'POST' is not supported for URL '" + queryPath + "'";

                given()
                    .header("Authorization", "Bearer " + validToken)
                .when()
                    .post(queryUrl)
                .then()
                    .body(
                        "messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage)
                    );
            }
        }
    }
    //@formatter:on
}
