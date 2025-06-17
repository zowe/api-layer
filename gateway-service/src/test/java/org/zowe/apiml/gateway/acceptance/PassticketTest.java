/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.ticket.TicketResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@AcceptanceTest
public class PassticketTest extends AcceptanceTestWithMockServices {

    private static final String USER_ID = "user";
    private static final String SERVICE_ID = "serviceusingpassticket";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String JWT = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNjcxNDYxNjIzLCJleHAiOjE2NzE0OTA0MjMsImlzcyI6IkFQSU1MIiwianRpIjoiYmFlMTkyZTYtYTYxMi00MThhLWI2ZGMtN2I0NWI5NzM4ODI3IiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIifQ.Vt5UjJUlbmuzmmEIodAACtj_AOxlsWqkFrFyWh4_MQRRPCj_zMIwnzpqRN-NJvKtUg1zxOCzXv2ypYNsglrXc7cH9wU3leK1gjYxK7IJjn2SBEb0dUL5m7-h4tFq2zNhcGH2GOmTpE2gTQGSTvDIdja-TIj_lAvUtbkiorm1RqrNu2MGC0WfgOGiak3tj2tNJLv_Y1ZMxNjzyHgXBMuNPozQrd4Vtnew3x4yy85LrTYF7jJM3U-e3AD2yImftxwycQvbkjNb-lWadejTVH0MgHMr04wVdDd8Nq5q7yrZf7YPzhias8ehNbew5CHiKut9SseZ1sO2WwgfhpEfsN4okg";
    private static final String PASSTICKET = "ZOWE_DUMMY_PASS_TICKET";

    @Test
    void whenRequestingPassticketForAllowedAPPLID_thenTranslate() throws IOException {
        TicketResponse response = new TicketResponse();
        response.setToken(JWT);
        response.setUserId(USER_ID);
        response.setApplicationName("IZUDFLT");
        response.setTicket(PASSTICKET);

        mockService("zaas").scope(MockService.Scope.TEST)
            .addEndpoint("/zaas/scheme/ticket")
            .assertion(he -> assertEquals(SERVICE_ID, he.getRequestHeaders().getFirst("X-Service-Id")))
            .assertion(he -> assertEquals(COOKIE_NAME + "=" + JWT, he.getRequestHeaders().getFirst("Cookie")))
            .bodyJson(response)
            .and().start();

        String expectedAuthHeader = "Basic " + Base64.getEncoder().encodeToString((USER_ID + ":" + PASSTICKET).getBytes(StandardCharsets.UTF_8));
        var mockService = mockService(SERVICE_ID).scope(MockService.Scope.TEST)
            .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET).applid("IZUDFLT")
            .addEndpoint("/" + SERVICE_ID + "/test")
            .assertion(he -> assertEquals(expectedAuthHeader, he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
            .and().start();

        given()
            .cookie(COOKIE_NAME, JWT)
            .when()
            .get(basePath + "/" + SERVICE_ID + "/api/v1/test")
            .then()
            .statusCode(Matchers.is(SC_OK));
        assertEquals(1, mockService.getEndpoint().getCounter());
    }

    @Test
    void whenCredentialsAreMissingOrInvalid_thenIgnoreTransformation() throws IOException {
        mockService("zaas").scope(MockService.Scope.TEST)
            .addEndpoint("/zaas/scheme/ticket")
            .responseCode(SC_UNAUTHORIZED)
            .and().start();
        var service = mockService(SERVICE_ID).scope(MockService.Scope.TEST)
            .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET).applid("IZUDFLT")
            .addEndpoint("/" + SERVICE_ID + "/test")
                .responseCode(SC_UNAUTHORIZED)
                .bodyJson(new ResponseDto("ok"))
            .assertion(he -> assertFalse(he.getRequestHeaders().containsKey(HttpHeaders.AUTHORIZATION)))
            .and().start();
        given()
            .cookie(COOKIE_NAME, JWT)
        .when()
            .get(basePath + "/" + SERVICE_ID + "/api/v1/test")
        .then()
            .statusCode(Matchers.is(SC_UNAUTHORIZED))
            .body("status", Matchers.is("ok"));
        assertEquals(1, service.getEndpoint().getCounter());
    }

    @Test
    void whenZaasIsMisconfigured_thenReturnError() throws IOException {
        var zaas = mockService("zaas").scope(MockService.Scope.TEST)
            .addEndpoint("/zaas/scheme/ticket")
            .responseCode(SC_INTERNAL_SERVER_ERROR)
            .and().start();
        var service = mockService(SERVICE_ID).scope(MockService.Scope.TEST)
            .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET).applid("IZUDFLT")
            .addEndpoint("/" + SERVICE_ID + "/test")
            .and().start();
        given()
            .cookie(COOKIE_NAME, JWT)
        .when()
            .get(basePath + "/" + SERVICE_ID + "/api/v1/test")
        .then()
            .statusCode(Matchers.is(SC_INTERNAL_SERVER_ERROR))
            .body("messages[0].messageKey", is("org.zowe.apiml.gateway.zaas.internalServerError"))
            .body("messages[0].messageContent", is("An internal exception occurred in ZAAS service " + zaas.getInstanceId() + "."));
        assertEquals(0, service.getEndpoint().getCounter());
    }

    @ParameterizedTest(name = "When ZAAS returns {0} the Gateway response with 503")
    @ValueSource(ints = {400, 403, 404, 405})
    void whenCannotGeneratePassticket_thenReturn503(int responseCode) throws IOException {
        mockService("zaas").scope(MockService.Scope.TEST)
            .addEndpoint("/zaas/scheme/ticket")
            .responseCode(responseCode)
            .and().start();
        var service = mockService(SERVICE_ID).scope(MockService.Scope.TEST)
            .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET).applid("IZUDFLT")
            .addEndpoint("/" + SERVICE_ID + "/test")
            .and().start();
        given()
            .cookie(COOKIE_NAME, JWT)
        .when()
            .get(basePath + "/" + SERVICE_ID + "/api/v1/test")
        .then()
            .statusCode(Matchers.is(SC_SERVICE_UNAVAILABLE))
            .body("messages[0].messageKey", is("org.zowe.apiml.common.serviceUnavailable"));
        assertEquals(0, service.getEndpoint().getCounter());
    }

    @Data
    @AllArgsConstructor
    static class ResponseDto {

        private String status;

    }

}
