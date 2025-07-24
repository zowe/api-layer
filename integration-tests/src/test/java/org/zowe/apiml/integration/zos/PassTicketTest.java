/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.zos;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
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
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_PASSTICKET;

/**
 * Verify integration of the API ML Passticket support with the zOS provider of the Passticket.
 */
@GeneralAuthenticationTest
class PassTicketTest implements TestWithStartedInstances {

    private static final EnvironmentConfiguration ENVIRONMENT_CONFIGURATION = ConfigReader.environmentConfiguration();
    private static final DiscoverableClientConfiguration DISCOVERABLE_CLIENT_CONFIGURATION =
        ENVIRONMENT_CONFIGURATION.getDiscoverableClientConfiguration();

    private static final String USERNAME = ENVIRONMENT_CONFIGURATION.getCredentials().getUser();
    private static final String APPLICATION_NAME = DISCOVERABLE_CLIENT_CONFIGURATION.getApplId();

    private static final String ZAAS_PASSTICKET_PATH = "/zaas/api/v1/auth/ticket";
    private static final String GATEWAY_PASSTICKET_PATH = "/gateway/api/v1/auth/ticket";

    private static final String COOKIE = "apimlAuthenticationToken";
    private URI url = HttpRequestUtils.getUriFromGateway(ROUTED_PASSTICKET);
    private static final boolean IS_MODULITH_ENABLED = Boolean.parseBoolean(System.getProperty("environment.modulith"));

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

                assertPassTicketIsValid(ticketResponse, jwt);
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

                assertPassTicketIsValid(ticketResponse, jwt);
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
                String expectedMessage;
                if (!IS_MODULITH_ENABLED) {
                    expectedMessage = "No authorization token provided for URL '" + ZAAS_PASSTICKET_PATH + "'";
                } else {
                    expectedMessage = "No authorization token provided for URL '" + GATEWAY_PASSTICKET_PATH + "'";
                }

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
                String expectedMessage = "The request has not been applied because it lacks valid authentication credentials.";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAO402E' }.messageContent", equalTo(expectedMessage));
            }

            @Test
            void givenInvalidTokenInHeader() {
                String jwt = "invalidToken";
                String expectedMessage = "The request has not been applied because it lacks valid authentication credentials.";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .header("Authorization", "Bearer " + jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAO402E' }.messageContent", equalTo(expectedMessage));
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
                    .contentType(JSON)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_BAD_REQUEST))
                    .body("messages.find { it.messageNumber == 'ZWEAG140E' }.messageContent", equalTo(expectedMessage));

            }

            @Test
            void givenInvalidApplicationName() {
                String expectedMessage = "The generation of the PassTicket failed. Reason:";
                TicketRequest ticketRequest = new TicketRequest(UNKNOWN_APPLID);

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_INTERNAL_SERVER_ERROR))
                    .body("messages.find { it.messageNumber == 'ZWEAG141E' }.messageContent", containsString(expectedMessage));
            }
        }

        @Nested
        class ReturnForbidden {
            @BeforeEach
            void resetCertsToNone() {
                RestAssured.config = RestAssuredConfig.newConfig().sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation());
            }

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
            @BeforeEach
            void setUpCertificate() {
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
            }

            @Test
            void givenInvalidHttpMethod() {
                String expectedMessage = "The request method has been disabled and cannot be used for the requested resource.";

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                .when()
                    .get(url)
                .then()
                    .statusCode(is(SC_METHOD_NOT_ALLOWED))
                    .body("messages.find { it.messageNumber == 'ZWEAO405E' }.messageContent", equalTo(expectedMessage));
            }
        }
    }

    private void assertPassTicketIsValid(TicketResponse ticketResponse, String jwt) {
        assertEquals(jwt, ticketResponse.getToken());
        assertEquals(USERNAME, ticketResponse.getUserId());
        assertEquals(APPLICATION_NAME, ticketResponse.getApplicationName());
    }
    //@formatter:on

}
