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
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import io.restassured.response.ValidatableResponseOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.ticket.TicketRequest;
import org.zowe.apiml.security.common.ticket.TicketResponse;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.DiscoverableClientConfiguration;
import org.zowe.apiml.util.config.EnvironmentConfiguration;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.util.Base64;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.zowe.apiml.gatewayservice.SecurityUtils.*;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID;

@Slf4j
public class PassTicketTest {

    private final static EnvironmentConfiguration ENVIRONMENT_CONFIGURATION = ConfigReader.environmentConfiguration();
    private final static GatewayServiceConfiguration GATEWAY_SERVICE_CONFIGURATION =
        ENVIRONMENT_CONFIGURATION.getGatewayServiceConfiguration();
    private final static DiscoverableClientConfiguration DISCOVERABLE_CLIENT_CONFIGURATION =
        ENVIRONMENT_CONFIGURATION.getDiscoverableClientConfiguration();

    private final static String SCHEME = GATEWAY_SERVICE_CONFIGURATION.getScheme();
    private final static String HOST = GATEWAY_SERVICE_CONFIGURATION.getHost();
    private final static int PORT = GATEWAY_SERVICE_CONFIGURATION.getPort();
    private final static String USERNAME = ENVIRONMENT_CONFIGURATION.getCredentials().getUser();
    private final static String PASSWORD = ENVIRONMENT_CONFIGURATION.getCredentials().getPassword();
    private final static String APPLICATION_NAME = DISCOVERABLE_CLIENT_CONFIGURATION.getApplId();
    private final static String DISCOVERABLECLIENT_PASSTICKET_BASE_PATH = "/api/v1/dcpassticket";
    private final static String DISCOVERABLECLIENT_BASE_PATH = "/api/v1/discoverableclient";
    private final static String PASSTICKET_TEST_ENDPOINT = "/passticketTest";
    private final static String TICKET_ENDPOINT = "/api/v1/gateway/auth/ticket";
    private final static String COOKIE = "apimlAuthenticationToken";
    private final static String REQUEST_INFO_ENDPOINT = "/request";

    @BeforeEach
    public void setUp() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @TestsNotMeantForZowe
    public void accessServiceWithCorrectPassTicket() {
        String jwt = gatewayToken();
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
        .when()
            .get(
                String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, PASSTICKET_TEST_ENDPOINT)
            )
        .then()
            .statusCode(is(SC_OK));
    }

    @Test
    @TestsNotMeantForZowe
    public void accessServiceWithIncorrectApplId() {
        String jwt = gatewayToken();
        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
        .when()
            .get(String.format("%s://%s:%d%s%s?applId=XBADAPPL",
                SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
            .then()
                .statusCode(is(SC_INTERNAL_SERVER_ERROR))
                .body("message", containsString("Error on evaluation of PassTicket"));
    }

    //@formatter:off
    @Test
    @TestsNotMeantForZowe
    public void accessServiceWithIncorrectToken() {
        String jwt = "nonsense";
        String expectedMessage = "Token is not valid";

        given()
            .cookie(GATEWAY_TOKEN_COOKIE_NAME, jwt)
        .when()
            .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, PASSTICKET_TEST_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED));
    }

    /*
     * /ticket endpoint tests
     */

    @Test
    @TestsNotMeantForZowe
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
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
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
    @TestsNotMeantForZowe
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
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
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
        String expectedMessage = "Authentication method 'GET' is not supported for URL '" + TICKET_ENDPOINT + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
        .when()
            .get(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_METHOD_NOT_ALLOWED))
            .body("messages.find { it.messageNumber == 'ZWEAG101E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doTicketWithoutCertificate() {
        String jwt = gatewayToken();
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_FORBIDDEN));
    }

    @Test
    public void doTicketWithoutToken() {
        String expectedMessage = "No authorization token provided for URL '" + TICKET_ENDPOINT + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAG131E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doTicketWithInvalidCookie() {
        String jwt = "invalidToken";
        String expectedMessage = "Token is not valid for URL '" + TICKET_ENDPOINT + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doTicketWithInvalidHeader() {
        String jwt = "invalidToken";
        String expectedMessage = "Token is not valid for URL '" + TICKET_ENDPOINT + "'";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        TicketRequest ticketRequest = new TicketRequest(APPLICATION_NAME);

        given()
            .contentType(JSON)
            .body(ticketRequest)
            .header("Authorization", "Bearer " + jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_UNAUTHORIZED))
            .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    public void doTicketWithoutApplicationName() {
        String expectedMessage = "The 'applicationName' parameter name is missing.";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String jwt = gatewayToken();

        given()
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body("messages.find { it.messageNumber == 'ZWEAG140E' }.messageContent", equalTo(expectedMessage));
    }

    @Test
    @TestsNotMeantForZowe
    public void doTicketWithInvalidApplicationName() {
        String expectedMessage = "The generation of the PassTicket failed. Reason: Unable to generate PassTicket. Verify that the secured signon (PassTicket) function and application ID is configured properly by referring to Using PassTickets in z/OS Security Server RACF Security Administrator's Guide.";

        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        String jwt = gatewayToken();
        TicketRequest ticketRequest = new TicketRequest(UNKNOWN_APPLID);

        given()
            .contentType(JSON)
            .body(ticketRequest)
            .cookie(COOKIE, jwt)
        .when()
            .post(String.format("%s://%s:%d%s", SCHEME, HOST, PORT, TICKET_ENDPOINT))
        .then()
            .statusCode(is(SC_BAD_REQUEST))
            .body("messages.find { it.messageNumber == 'ZWEAG141E' }.messageContent", equalTo(expectedMessage));

    }

    private <T extends ValidatableResponseOptions<T, R>, R extends ResponseBody<R> & ResponseOptions<R>>
        void verifyPassTicketHeaders(T v)
    {
        String basic = "Basic " + Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes());
        v   .statusCode(200)
            .body("headers.authorization", not(startsWith("Bearer ")))
            .body("headers.authorization", startsWith("Basic "))
            .body("headers.authorization", not(equals(basic)))
            .body("cookies", not(hasKey(COOKIE)));
    }

    @Test
    @TestsNotMeantForZowe
    void givenBearerJwt_whenUsePassticketsAuthenticationScheme_thenResultContainsPassticketAndNoJwt() {
        verifyPassTicketHeaders(
            given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gatewayToken())
            .when()
                .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, REQUEST_INFO_ENDPOINT))
            .then()
        );
    }

    @Test
    @TestsNotMeantForZowe
    void givenCookieJwt_whenUsePassticketsAuthenticationScheme_thenResultContainsPassticketAndNoJwt() {
        verifyPassTicketHeaders(
            given()
                .cookie(COOKIE, gatewayToken())
            .when()
                .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, REQUEST_INFO_ENDPOINT))
            .then()
        );
    }

    @Test
    @TestsNotMeantForZowe
    void givenBasicAuth_whenUsePassticketsAuthenticationScheme_thenResultContainsPassticketAndNoJwt() {
        verifyPassTicketHeaders(
            given()
                .auth().preemptive().basic(USERNAME, PASSWORD)
            .when()
                .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, REQUEST_INFO_ENDPOINT))
            .then()
        );
    }

    @Test
    @TestsNotMeantForZowe
    void givenBothJwt_whenUsePassticketsAuthenticationScheme_thenResultContainsPassticketAndNoJwt() {
        verifyPassTicketHeaders(
            given()
                .cookie(COOKIE, gatewayToken())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gatewayToken())
            .when()
                .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, REQUEST_INFO_ENDPOINT))
            .then()
        );
    }

    @Test
    @TestsNotMeantForZowe
    void givenBasicAndCookieJwt_whenUsePassticketsAuthenticationScheme_thenResultContainsPassticketAndNoJwt() {
        verifyPassTicketHeaders(
            given()
                .auth().preemptive().basic(USERNAME, PASSWORD)
                .cookie(COOKIE, gatewayToken())
            .when()
                .get(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, DISCOVERABLECLIENT_PASSTICKET_BASE_PATH, REQUEST_INFO_ENDPOINT))
            .then()
        );
    }
    //@formatter:on

}
