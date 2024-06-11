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
import io.restassured.http.Header;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import io.restassured.response.ValidatableResponseOptions;
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.integration.penetration.JwtPenTest.getToken;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.PASSTICKET_TEST_ENDPOINT;
import static org.zowe.apiml.util.requests.Endpoints.REQUEST_INFO_ENDPOINT;

@DiscoverableClientDependentTest
@GeneralAuthenticationTest
public class PassticketSchemeTest implements TestWithStartedInstances {
    private final static URI requestUrl = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    private final static URI discoverablePassticketUrl = HttpRequestUtils.getUriFromGateway(PASSTICKET_TEST_ENDPOINT);
    static GatewayServiceConfiguration conf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

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

    static final Set<String> scopes = new HashSet<>();
    static final String jwt;
    static final String pat;

    static {
        scopes.add("dcpassticket");
        jwt = gatewayToken();
        pat = personalAccessToken(scopes);
    }

    static Stream<Arguments> accessTokens() {
        return Stream.of(
            Arguments.of(jwt, COOKIE_NAME, new Header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)),
            Arguments.of(pat, COOKIE_NAME, new Header(HttpHeaders.AUTHORIZATION, "Bearer " + pat)),
            Arguments.of(pat, PAT_COOKIE_AUTH_NAME, new Header(ApimlConstants.PAT_HEADER_NAME, pat))
        );
    }

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class GivenGatewayUrlTests {
        @Test
        @Tag("GatewayServiceRouting")
        void givenValidJWT_thenTranslateToPassticket() {
            String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), REQUEST_INFO_ENDPOINT);
            verifyPassTicketHeaders(
                given().cookie(COOKIE_NAME, jwt)
                    .when()
                    .get(scgUrl)
                    .then()
            );
        }

        @Test
        @Tag("GatewayServiceRouting")
        void givenNoJWT_thenErrorHeaderIsCreated() {
            String scgUrl = String.format("%s://%s:%s%s", conf.getScheme(), conf.getHost(), conf.getPort(), REQUEST_INFO_ENDPOINT);
            given()
                .when()
                .get(scgUrl)
                .then()
                .statusCode(SC_OK)
                .body("headers.x-zowe-auth-failure", startsWith("ZWEAG141E"))
                .header(ApimlConstants.AUTH_FAIL_HEADER, startsWith("ZWEAG141E"));
        }
    }

    @Nested
    class WhenUsingPassticketAuthenticationScheme {
        @Nested
        class ResultContainsPassticketAndNoJwt {
            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            @InfinispanStorageTest
            void givenJwtInBearerHeader(String token, String cookie, Header header) {
                verifyPassTicketHeaders(
                    given()
                        .header(header)
                        .when()
                        .get(requestUrl)
                        .then()
                );

            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            @InfinispanStorageTest
            void givenJwtInCookie(String token, String cookie) {

                verifyPassTicketHeaders(
                    given()
                        .cookie(cookie, token)
                        .when()
                        .get(requestUrl)
                        .then()
                );

            }

            @Test
            void givenBasicAuth() {
                verifyPassTicketHeaders(
                    given()
                        .auth().preemptive().basic(USERNAME, new String(PASSWORD))
                        .when()
                        .get(requestUrl)
                        .then()
                );
            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            @InfinispanStorageTest
            void givenJwtInHeaderAndCookie(String token, String cookie, Header header) {

                verifyPassTicketHeaders(
                    given()
                        .cookie(cookie, token)
                        .header(header)
                        .when()
                        .get(requestUrl)
                        .then()
                );

            }

            @ParameterizedTest
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#accessTokens")
            @InfinispanStorageTest
            void givenBasicAndJwtInCookie(String token, String cookie) {

                verifyPassTicketHeaders(
                    given()
                        .auth().preemptive().basic(USERNAME, new String(PASSWORD))
                        .cookie(cookie, token)
                        .when()
                        .get(requestUrl)
                        .then()
                );

            }

            @ParameterizedTest(name = "call passticket service with {0} to receive response code {2}")
            @MethodSource("org.zowe.apiml.integration.authentication.schemes.PassticketSchemeTest#getTokens")
            @InfinispanStorageTest
            @TestsNotMeantForZowe
            void whenCallPassTicketService(String tokenType, int status) throws JSONException {
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
            @InfinispanStorageTest
            void givenCorrectToken(String token, String cookie) {
                given()
                    .cookie(cookie, token)
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
            @InfinispanStorageTest
            void givenIssuedForIncorrectApplId(String token, String cookie) {
                String expectedMessage = "Error on evaluation of PassTicket";

                URI discoverablePassticketUrl = HttpRequestUtils.getUriFromGateway(
                    PASSTICKET_TEST_ENDPOINT,
                    Collections.singletonList(new BasicNameValuePair("applId", "XBADAPPL"))
                );

                given()
                    .cookie(cookie, token)
                    .when()
                    .get(discoverablePassticketUrl)
                    .then()
                    .statusCode(is(SC_INTERNAL_SERVER_ERROR))
                    .body("message", containsString(expectedMessage));

            }
        }

        @Nested
        class StorePassTicketInHeader {

            @Test
            void givenCustomHeader() {
                given()
                    .cookie(COOKIE_NAME, jwt)
                .when()
                    .get(requestUrl)
                .then()
                    .body("headers.custompassticketheader", Matchers.notNullValue())
                    .body("headers.customuserheader", Matchers.notNullValue())
                    .statusCode(200);
            }

        }

    }

    private <T extends ValidatableResponseOptions<T, R>, R extends ResponseBody<R> & ResponseOptions<R>>
    void verifyPassTicketHeaders(T v) {
        String basic = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
        v
            .body("headers.authorization", not(startsWith("Bearer ")))
            .body("headers.authorization", startsWith("Basic "))
            .body("headers.authorization", not(equals(basic)))
            .body("cookies", not(hasKey(COOKIE_NAME)))
            .statusCode(200);
    }
}
