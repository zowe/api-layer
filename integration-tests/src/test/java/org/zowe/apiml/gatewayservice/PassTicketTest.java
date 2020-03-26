/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.zowe.apiml.security.common.ticket.TicketRequest;
import org.zowe.apiml.security.common.ticket.TicketResponse;
import org.zowe.apiml.util.config.ConfigReader;

import java.util.Arrays;
import java.util.Collection;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.zowe.apiml.gatewayservice.SecurityUtils.*;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID;

@Slf4j
@RunWith(value = Parameterized.class)
public class PassTicketTest {

    private static final String SCHEME = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
        .getScheme();
    private static final String HOST = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration()
        .getHost();
    private static final int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();
    private static final String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String APPLICATION_NAME = ConfigReader.environmentConfiguration()
        .getDiscoverableClientConfiguration().getApplId();
    private static final String STATICCLIENT_BASE_PATH = "/api/v1/staticclient";
    private static final String DISCOVERABLECLIENT_BASE_PATH = "/api/v1/discoverableclient";
    private static final String PASSTICKET_TEST_ENDPOINT = "/passticketTest";
    private static final String COOKIE = "apimlAuthenticationToken";
    private static final String BASE_PATH = "/api/v1/gateway";
    private static final String END_POINT = "/auth/ticket";

    private String ticketEndpoint;

    public PassTicketTest(String ticketEndpoint) {
        this.ticketEndpoint = ticketEndpoint;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {END_POINT},
            {BASE_PATH + END_POINT}
        });
    }

    private boolean rejectedOnZull() {
        return ticketEndpoint.startsWith(BASE_PATH);
    }

    @Before
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    public void accessServiceWithCorrectPassTicket() {
        String jwt = gatewayToken();
        given().cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt).when().get(
            String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
            .then().statusCode(is(SC_OK));
    }

    @Test
    public void accessServiceWithIncorrectApplId() {
        String jwt = gatewayToken();
        given().cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt).when()
            .get(String.format("%s://%s:%d%s%s?applId=XBADAPPL", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH,
                PASSTICKET_TEST_ENDPOINT))
            .then().statusCode(is(SC_INTERNAL_SERVER_ERROR))
            .body("message", containsString("Error on evaluation of PassTicket"));
    }

    //@formatter:off
    @Test
    public void accessServiceWithIncorrectToken() {
        String jwt = "nonsense";
        String expectedMessage = "Token is not valid";

        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, STATICCLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAG102E' }.messageContent", equalTo(expectedMessage));
    }

    /*
     * /ticket endpoint tests
     */

    @Test
    public void doTicketWithValidCookieAndCertificate() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String jwt = gatewayToken();
        log.info(APPLICATION_NAME);
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        // Generate ticket
        TicketResponse ticketResponse = given()
            .contentType(JSON)
            .body(ticketRequest)
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_OK))
            .extract().body().as(TicketResponse.class);

        assertEquals(jwt, ticketResponse.getToken());
        assertEquals(USERNAME, ticketResponse.getUserId());
        assertEquals(APPLICATION_NAME, ticketResponse.getApplicationName());

        // Validate ticket
        given()
             .auth().preemptive().basic(USERNAME, ticketResponse.getTicket())
             .param("appliId", APPLICATION_NAME)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void doTicketWithValidHeaderAndCertificate() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String jwt = gatewayToken();
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        //Generate ticket
        TicketResponse ticketResponse = given()
            .contentType(JSON)
            .body(ticketRequest)
            .header("Authorization", "Bearer " + jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_OK))
            .extract().body().as(TicketResponse.class);

        assertEquals(jwt, ticketResponse.getToken());
        assertEquals(USERNAME, ticketResponse.getUserId());
        assertEquals(APPLICATION_NAME, ticketResponse.getApplicationName());

        // Validate ticket
        given()
            .auth().preemptive().basic(USERNAME, ticketResponse.getTicket())
            .param("appliId", APPLICATION_NAME)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    public void doTicketWithInvalidMethod() {
        String expectedMessage = "Authentication method 'GET' is not supported for URL '" + END_POINT + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body("messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doTicketWithoutToken() {
        String expectedMessage = "No authorization token provided for URL '" + ticketEndpoint + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        ValidatableResponse vr = given()
            .contentType(JSON)
            .body(ticketRequest)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));

        if (!rejectedOnZull()) {
            vr.body("messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage));
        }
    }

    @Test
    public void doTicketWithInvalidCookie() {
        String jwt = "invalidToken";
        String expectedMessage = "Token is not valid for URL '" + ticketEndpoint + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doTicketWithInvalidHeader() {
        String jwt = "invalidToken";
        String expectedMessage = "Token is not valid for URL '" + ticketEndpoint + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        ValidatableResponse vr = given()
            .contentType(JSON)
            .body(ticketRequest)
            .header("Authorization", "Bearer " + jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));

        if (!rejectedOnZull()) {
            vr.body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
        }
    }

    @Test
    public void doTicketWithoutApplicationName() {
        String expectedMessage = "The 'applicationName' parameter name is missing.";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String jwt = gatewayToken();

        ValidatableResponse vr = given()
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_BAD_REQUEST));

        if (!rejectedOnZull()) {
             vr.body("messages.find { it.messageNumber == 'ZWEAG140E' }.messageContent", equalTo(expectedMessage));
        }
    }

    @Test
    public void doTicketWithInvalidApplicationName() {
        String expectedMessage = "The generation of the PassTicket failed. Reason: Unable to generate PassTicket. Verify that the secured signon (PassTicket) function and application ID is configured properly by referring to Using PassTickets in z/OS Security Server RACF Security Administrator's Guide.";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String jwt = gatewayToken();
        TicketRequest ticketRequest = new TicketRequest(UNKNOWN_APPLID);

        ValidatableResponse vr = given()
            .contentType(JSON)
            .body(ticketRequest)
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, ticketEndpoint))
        .then()
            .statusCode(is(SC_BAD_REQUEST));

        if (!rejectedOnZull()) {
            vr.body("messages.find { it.messageNumber == 'ZWEAG141E' }.messageContent", equalTo(expectedMessage));
        }
    }
    //@formatter:on
}
