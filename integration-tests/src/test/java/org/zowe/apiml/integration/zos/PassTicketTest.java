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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.security.common.ticket.TicketRequest;
import org.zowe.apiml.security.common.ticket.TicketResponse;
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
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID;
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

    private final static String TICKET_ENDPOINT = "/gateway/api/v1/auth/ticket";
    private final static String TICKET_ENDPOINT_OLD_PATH_FORMAT = "/api/v1/gateway/auth/ticket";
    private final static String COOKIE = "apimlAuthenticationToken";

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected static URI[] passTicketUrls() {
        return new URI[]{
            HttpRequestUtils.getUriFromGateway(TICKET_ENDPOINT),
            HttpRequestUtils.getUriFromGateway(TICKET_ENDPOINT_OLD_PATH_FORMAT)
        };
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

            @ParameterizedTest(name = "givenValidTokenInCookieAndCertificate {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenValidTokenInCookieAndCertificate(URI url) {
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

            @ParameterizedTest(name = "givenValidTokenInHeaderAndCertificate {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenValidTokenInHeaderAndCertificate(URI url) {
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

            @ParameterizedTest(name = "givenNoToken {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenNoToken(URI url) {
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

            @ParameterizedTest(name = "givenInvalidTokenInCookie {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenInvalidTokenInCookie(URI url) {
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

            @ParameterizedTest(name = "givenInvalidTokenInHeader {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenInvalidTokenInHeader(URI url) {
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

            @ParameterizedTest(name = "givenNoApplicationName {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenNoApplicationName(URI url) {
                String expectedMessage = "The 'applicationName' parameter name is missing.";

                given()
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_BAD_REQUEST))
                    .body("messages.find { it.messageNumber == 'ZWEAG140E' }.messageContent", equalTo(expectedMessage));

            }

            @ParameterizedTest(name = "givenInvalidApplicationName {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenInvalidApplicationName(URI url) {
                String expectedMessage = "The generation of the PassTicket failed. Reason: Unable to generate PassTicket. Verify that the secured signon (PassTicket) function and application ID is configured properly by referring to Using PassTickets in z/OS Security Server RACF Security Administrator's Guide.";
                TicketRequest ticketRequest = new TicketRequest(UNKNOWN_APPLID);

                given()
                    .contentType(JSON)
                    .body(ticketRequest)
                    .cookie(COOKIE, jwt)
                .when()
                    .post(url)
                .then()
                    .statusCode(is(SC_BAD_REQUEST))
                    .body("messages.find { it.messageNumber == 'ZWEAG141E' }.messageContent", equalTo(expectedMessage));

            }
        }

        @Nested
        class ReturnForbidden {
            @ParameterizedTest(name = "givenNoCertificate {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenNoCertificate(URI url) {
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
            @ParameterizedTest(name = "givenInvalidHttpMethod {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.zos.PassTicketTest#passTicketUrls")
            void givenInvalidHttpMethod(URI url) {
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

    private void assertPassTicketIsValid(TicketResponse ticketResponse, String jwt) {
        assertEquals(jwt, ticketResponse.getToken());
        assertEquals(USERNAME, ticketResponse.getUserId());
        assertEquals(APPLICATION_NAME, ticketResponse.getApplicationName());
    }
    //@formatter:on

}
