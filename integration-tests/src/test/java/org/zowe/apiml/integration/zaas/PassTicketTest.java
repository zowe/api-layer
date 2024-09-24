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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.ZaasTest;
import org.zowe.apiml.util.config.ConfigReader;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.XML;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.integration.zaas.ZaasTestUtil.COOKIE;
import static org.zowe.apiml.integration.zaas.ZaasTestUtil.ZAAS_TICKET_URI;
import static org.zowe.apiml.util.SecurityUtils.USERNAME;
import static org.zowe.apiml.util.SecurityUtils.generateZoweJwtWithLtpa;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;
import static org.zowe.apiml.util.SecurityUtils.getZosmfJwtTokenFromGw;
import static org.zowe.apiml.util.SecurityUtils.getZosmfLtpaToken;
import static org.zowe.apiml.util.SecurityUtils.personalAccessToken;
import static org.zowe.apiml.util.SecurityUtils.validOktaAccessToken;

/**
 * Verify integration of the API ML PassTicket support with the zOS provider of the PassTicket.
 */
@ZaasTest
class PassTicketTest implements TestWithStartedInstances {

    private final static String APPLICATION_NAME = ConfigReader.environmentConfiguration().getDiscoverableClientConfiguration().getApplId();

    @BeforeEach
    void setUpCertificate() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class WhenGeneratingPassTicket_returnValidPassTicket {

        private final TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        @Test
        void givenValidZosmfToken() {
            String zosmfToken = getZosmfJwtTokenFromGw();

            //@formatter:off
            given()
                .cookie(COOKIE, zosmfToken)
                .body(ticketRequest)
                .contentType(JSON)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(SC_OK)
                .body("ticket", not(isEmptyOrNullString()))
                .body("userId", is(USERNAME))
                .body("applicationName", is(APPLICATION_NAME));
            //@formatter:on
        }

        @Test
        void givenValidZoweTokenWithLtpa() throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
            String ltpaToken = getZosmfLtpaToken();
            String zoweToken = generateZoweJwtWithLtpa(ltpaToken);

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + zoweToken)
                .body(ticketRequest)
                .contentType(JSON)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(SC_OK)
                .body("ticket", not(isEmptyOrNullString()))
                .body("userId", is(USERNAME))
                .body("applicationName", is(APPLICATION_NAME));
            //@formatter:on
        }

        @Test
        void givenValidPersonalAccessToken() {
            String serviceId = "gateway";
            String pat = personalAccessToken(Collections.singleton(serviceId));

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + pat)
                .header("X-Service-Id", serviceId)
                .body(ticketRequest)
                .contentType(JSON)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(SC_OK)
                .body("ticket", not(isEmptyOrNullString()))
                .body("userId", is(USERNAME))
                .body("applicationName", is(APPLICATION_NAME));
            //@formatter:on
        }

        @ParameterizedTest(name = "PassTicketTest.givenX509Certificate {1}")
        @MethodSource("org.zowe.apiml.integration.zaas.ZaasTestUtil#provideClientCertificates")
        void givenX509Certificate(String certificate, String description) {
            //@formatter:off
            given()
                .header("Client-Cert", certificate)
                .body(ticketRequest)
                .contentType(JSON)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(SC_OK)
                .body("ticket", not(isEmptyOrNullString()))
                .body("userId", not(isEmptyOrNullString()))
                .body("applicationName", is(APPLICATION_NAME));
            //@formatter:on
        }

        @Test
        void givenValidOAuthToken() {
            String oAuthToken = validOktaAccessToken(true);

            //@formatter:off
            given()
                .cookie(COOKIE, oAuthToken)
                .body(ticketRequest)
                .contentType(JSON)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(SC_OK)
                .body("ticket", not(isEmptyOrNullString()))
                .body("userId", is(USERNAME))
                .body("applicationName", is(APPLICATION_NAME));
            //@formatter:on
        }
    }

    @Nested
    class WhenGeneratingPassTicket_returnBadRequest {

        private final String jwt = getZosmfJwtTokenFromGw();

        @Test
        void givenNoApplicationName() {
            String expectedMessage = "The 'applicationName' parameter name is missing.";

            //@formatter:off
            given()
                .contentType(JSON)
                .body(new TicketRequest())
                .cookie(COOKIE, jwt)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(is(SC_BAD_REQUEST))
                .body("messages.find { it.messageNumber == 'ZWEAG140E' }.messageContent", equalTo(expectedMessage));
            //@formatter:on
        }

        @Test
        void givenInvalidApplicationName() {
            String expectedMessage = "The generation of the PassTicket failed. Reason:";
            TicketRequest ticketRequest = new TicketRequest(PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID);

            //@formatter:off
            given()
                .contentType(JSON)
                .body(ticketRequest)
                .cookie(COOKIE, jwt)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(is(SC_BAD_REQUEST))
                .body("messages.find { it.messageNumber == 'ZWEAG141E' }.messageContent", containsString(expectedMessage));
            //@formatter:on
        }

        @Test
        @Disabled("Enable once it runs on z/OS. Mimic the behaviour in Mock service.")
        void givenLongApplicationName() {
            //@formatter:off
            given()
                .body(new TicketRequest("TooLongAppName"))
                .cookie(COOKIE, jwt)
                .contentType(JSON)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(is(SC_BAD_REQUEST));
            //@formatter:on
        }

        @Test
        void givenNoContentType() {
            //@formatter:off
             given()
                .body(new TicketRequest(APPLICATION_NAME))
                .cookie(COOKIE, jwt)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(is(SC_NOT_FOUND));
            //@formatter:on
        }

        @Test
        void givenInvalidContentType() {
            //@formatter:off
            given()
                .body(new TicketRequest(APPLICATION_NAME))
                .cookie(COOKIE, jwt)
                .contentType(XML)
            .when()
                .post(ZAAS_TICKET_URI)
            .then()
                .statusCode(is(SC_NOT_FOUND));
            //@formatter:on
        }

    }

    // Additional negative tests are in ZaasNegativeTest since they are common for the whole service

}
