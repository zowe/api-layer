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
import org.apache.http.HttpHeaders;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.MainframeDependentTests;
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
import static org.zowe.apiml.util.SecurityUtils.*;

@DiscoverableClientDependentTest
@GeneralAuthenticationTest
public class PassticketSchemeTest {
    private final static String REQUEST_INFO_ENDPOINT = "/api/v1/dcpassticket/request";
    private final static String PASSTICKET_TEST_ENDPOINT = "/api/v1/dcpassticket/passticketTest";

    private final static URI requestUrl = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    private final static URI discoverablePassticketUrl = HttpRequestUtils.getUriFromGateway(PASSTICKET_TEST_ENDPOINT);

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class WhenUsingPassticketAuthenticationScheme {
        @Nested
        class ResultContainsPassticketAndNoJwt {
            @Test
            void givenJwtInBearerHeader() {
                String jwt = gatewayToken();

                verifyPassTicketHeaders(
                    given()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }

            @Test
            void givenJwtInCookie() {
                String jwt = gatewayToken();

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

            @Test
            void givenJwtInHeaderAndCookie() {
                String jwt = gatewayToken();

                verifyPassTicketHeaders(
                    given()
                        .cookie(COOKIE_NAME, jwt)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }

            @Test
            void givenBasicAndJwtInCookie() {
                String jwt = gatewayToken();

                verifyPassTicketHeaders(
                    given()
                        .auth().preemptive().basic(USERNAME, PASSWORD)
                        .cookie(COOKIE_NAME, jwt)
                    .when()
                        .get(requestUrl)
                    .then()
                );

            }
        }

        @Nested
        class VerifyPassTicketIsOk {
            @Test
            void givenCorrectToken() {
                String jwt = gatewayToken();
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
            @Test
            @MainframeDependentTests // The appl id needs to be verified against actual ESM
            void givenIssuedForIncorrectApplId() {
                String jwt = gatewayToken();
                String expectedMessage = "Error on evaluation of PassTicket";

                URI discoverablePassticketUrl = HttpRequestUtils.getUriFromGateway(
                    PASSTICKET_TEST_ENDPOINT,
                    Collections.singletonList(new BasicNameValuePair("applid", "XBADAPPL"))
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
        v   .statusCode(200)
            .body("headers.authorization", not(startsWith("Bearer ")))
            .body("headers.authorization", startsWith("Basic "))
            .body("headers.authorization", not(equals(basic)))
            .body("cookies", not(hasKey(COOKIE_NAME)));
    }
}
