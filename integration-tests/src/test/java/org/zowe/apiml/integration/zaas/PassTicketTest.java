/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.zaas;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoverableClientConfiguration;
import org.zowe.apiml.util.config.EnvironmentConfiguration;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify integration of the API ML Passticket support with the zOS provider of the Passticket.
 */
@Slf4j
@GeneralAuthenticationTest
class PassTicketTest implements TestWithStartedInstances {

    private final static EnvironmentConfiguration ENVIRONMENT_CONFIGURATION = ConfigReader.environmentConfiguration();
    private final static DiscoverableClientConfiguration DISCOVERABLE_CLIENT_CONFIGURATION =
        ENVIRONMENT_CONFIGURATION.getDiscoverableClientConfiguration();

    private final static String USERNAME = ENVIRONMENT_CONFIGURATION.getCredentials().getUser();
    private final static String APPLICATION_NAME = DISCOVERABLE_CLIENT_CONFIGURATION.getApplId();

    private final static String COOKIE = "apimlAuthenticationToken";
    private URI url = HttpRequestUtils.getUriFromGateway("/gateway/zaas/ticket");

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class WhenGeneratingPassticket {
        private String jwt;
        private TicketRequest ticketRequest;

        @BeforeEach
        void setUpToken() {
            jwt = gatewayToken();
            ticketRequest = new TicketRequest(APPLICATION_NAME);
        }

        @Nested
        class ReturnValidPassticket {
            @BeforeEach
            void setUpCertificate() {
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
            }

            @Test
            void givenValidTokenInCookieAndCertificate() {
                TicketResponse ticketResponse = given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_OK))
                    .extract().body().as(TicketResponse.class);

                assertPassTicketIsValid(ticketResponse);
            }

            @Test
            void givenValidTokenInHeaderAndCertificate() {
                TicketResponse ticketResponse = given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .header("Authorization", "Bearer " + jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_OK))
                    .extract().body().as(TicketResponse.class);

                assertPassTicketIsValid(ticketResponse);
            }
        }

        @Nested
        class ReturnUnauthorized {
            @BeforeEach
            void setUpCertificateAndToken() {
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
            }

            @Test
            void givenNoToken() {
                String expectedMessage = "No authorization token provided for URL '" + url.getPath() + "'";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage));
            }

            @Test
            void givenInvalidTokenInCookie() {
                String jwt = "invalidToken";
                String expectedMessage = "Token is not valid for URL '" + url.getPath() + "'";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
            }

            @Test
            void givenInvalidTokenInHeader() {
                String jwt = "invalidToken";
                String expectedMessage = "Token is not valid for URL '" + url.getPath() + "'";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .header("Authorization", "Bearer " + jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
            }
        }

        @Nested
        class ReturnBadRequest {
            @BeforeEach
            void setUpCertificateAndToken() {
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
            }

            @Test
            void givenNoApplicationName() {
                String expectedMessage = "The 'applicationName' parameter name is missing.";

                given()
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_BAD_REQUEST))
                    .body("messages.find { it.messageNumber == 'ZWEAG140E' }.messageContent", equalTo(expectedMessage));

            }

//            @Test
//            void givenInvalidApplicationName() {
//                String expectedMessage = "The generation of the PassTicket failed. Reason: Unable to generate PassTicket. Verify that the secured signon (PassTicket) function and application ID is configured properly by referring to Using PassTickets in z/OS Security Server RACF Security Administrator's Guide.";
//                TicketRequest ticketRequest = new TicketRequest(UNKNOWN_APPLID);
//
//                given()
//                    .contentType(JSON)
//                    .body(ticketRequest)
//                    .cookie(COOKIE, jwt)
//                .when()
//                    .post(url)
//                .then()
//                    .statusCode(is(SC_BAD_REQUEST))
//                    .body("messages.find { it.messageNumber == 'ZWEAG141E' }.messageContent", equalTo(expectedMessage));
//
//            }
        }

        @Nested
        class ReturnForbidden {
            @Test
            void givenNoCertificate() {
                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_FORBIDDEN));
            }
        }

        @Nested
        class ReturnMethodNotAllowed {
            @Test
            void givenInvalidHttpMethod() {
                String expectedMessage = "Authentication method 'GET' is not supported for URL '" + url.getPath() + "'";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                .when()
                    .get(url)
                .then()
                    .statusCode(is(SC_METHOD_NOT_ALLOWED))
                    .body("messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage));
            }
        }
    }

    private void assertPassTicketIsValid(TicketResponse ticketResponse) {
        assertEquals(USERNAME, ticketResponse.getUserId());
        assertEquals(APPLICATION_NAME, ticketResponse.getApplicationName());
    }
    //@formatter:on

}
