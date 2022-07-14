/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.authentication.schemes;

import io.restassured.RestAssured;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import io.restassured.response.ValidatableResponseOptions;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.integration.penetration.JwtPenTest.getToken;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

@DiscoverableClientDependentTest
@GeneralAuthenticationTest
public class PassticketSchemeTest implements TestWithStartedInstances {
    private final static URI requestUrl = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    private final static URI discoverablePassticketUrl = HttpRequestUtils.getUriFromGateway(PASSTICKET_TEST_ENDPOINT);

    public static Stream<Arguments> getTokens() {
        return Stream.of(
            Arguments.of("validJwt", SC_OK),
            Arguments.of("noSignJwt", SC_OK),
            Arguments.of("publicKeySignedJwt", SC_OK),
            Arguments.of("changedRealmJwt", SC_OK),
            Arguments.of("changedUserJwt", SC_OK),
            Arguments.of("personalAccessToken", SC_OK)
        );
    }
    static Set<String> scopes = new HashSet<>();
    static String jwt;
    static String pat;
    static {
        scopes.add("dcpassticket");
        jwt = gatewayToken();
        pat = personalAccessToken(scopes);
    }
    private static Stream<String> accessTokens(){
        return Stream.of(jwt);
    }
    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class WhenUsingPassticketAuthenticationScheme {
        @Nested
        class ResultContainsPassticketAndNoJwt {
            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            void givenJwtInBearerHeader(String jwt) {
                verifyPassTicketHeaders(
                    given()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            void givenJwtInCookie(String jwt) {

                verifyPassTicketHeaders(
                    given()
                        .cookie(COOKIE_NAME, jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }

            @Test
            void givenBasicAuth() {
                verifyPassTicketHeaders(
                    given()
                        .auth().preemptive().basic(USERNAME, PASSWORD)
                    .when()
                        .get(requestUrl)
                    .then()
                );
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            void givenJwtInHeaderAndCookie(String jwt) {

                verifyPassTicketHeaders(
                    given()
                        .cookie(COOKIE_NAME, jwt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            void givenBasicAndJwtInCookie(String jwt) {

                verifyPassTicketHeaders(
                    given()
                        .auth().preemptive().basic(USERNAME, PASSWORD)
                        .cookie(COOKIE_NAME, jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }

            @ParameterizedTest(name = "call passticket service with {0} to receive response code {2}")
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#getTokens")
            @TestsNotMeantForZowe
            void whenCallPassTicketService(String tokenType, int status) {
                String token = getToken(tokenType);

                //@formatter:off
                given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(discoverablePassticketUrl)
                    .then()
                    .statusCode(is(status));
                //@formatter:on

            }
        }

        @Nested
        class VerifyPassTicketIsOk {
            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            void givenCorrectToken(String jwt) {
                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
                .when()
                    .get(
                        discoverablePassticketUrl
                    )
                .then()
                    .statusCode(is(SC_OK));
            }
        }

        @Nested
        class VerifyPassTicketIsInvalid {

            @MainframeDependentTests // The appl id needs to be verified against actual ESM
            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            void givenIssuedForIncorrectApplId(String jwt) {
                String expectedMessage = "Error on evaluation of PassTicket";

                URI discoverablePassticketUrl = HttpRequestUtils.getUriFromGateway(
                    PASSTICKET_TEST_ENDPOINT,
                    Collections.singletonList(new BasicNameValuePair("applId", "XBADAPPL"))
                );

                given()
                    .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
                .when()
                    .get(discoverablePassticketUrl)
                .then()
                    .statusCode(is(SC_INTERNAL_SERVER_ERROR))
                    .body("message", containsString(expectedMessage));

            }
        }
    }

    private <T extends ValidatableResponseOptions<T, R>, R extends ResponseBody<R> & ResponseOptions<R>>
    void verifyPassTicketHeaders(T v)
    {
        String basic = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
        v
            .body("headers.authorization", not(startsWith("Bearer ")))
            .body("headers.authorization", startsWith("Basic "))
            .body("headers.authorization", not(equals(basic)))
            .body("cookies", not(hasKey(COOKIE_NAME)))
            .statusCode(200);
    }
}
