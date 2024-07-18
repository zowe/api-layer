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

import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MockService;
import org.zowe.apiml.ticket.TicketResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

@AcceptanceTest
public class PassticketTest extends AcceptanceTestWithMockServices {

    private static final String USER_ID = "user";
    private static final String SERVICE_ID = "serviceusingpassticket";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String JWT = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNjcxNDYxNjIzLCJleHAiOjE2NzE0OTA0MjMsImlzcyI6IkFQSU1MIiwianRpIjoiYmFlMTkyZTYtYTYxMi00MThhLWI2ZGMtN2I0NWI5NzM4ODI3IiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIifQ.Vt5UjJUlbmuzmmEIodAACtj_AOxlsWqkFrFyWh4_MQRRPCj_zMIwnzpqRN-NJvKtUg1zxOCzXv2ypYNsglrXc7cH9wU3leK1gjYxK7IJjn2SBEb0dUL5m7-h4tFq2zNhcGH2GOmTpE2gTQGSTvDIdja-TIj_lAvUtbkiorm1RqrNu2MGC0WfgOGiak3tj2tNJLv_Y1ZMxNjzyHgXBMuNPozQrd4Vtnew3x4yy85LrTYF7jJM3U-e3AD2yImftxwycQvbkjNb-lWadejTVH0MgHMr04wVdDd8Nq5q7yrZf7YPzhias8ehNbew5CHiKut9SseZ1sO2WwgfhpEfsN4okg";
    private static final String PASSTICKET = "ZOWE_DUMMY_PASS_TICKET";

    @BeforeEach
    void setup() throws IOException {
        TicketResponse response = new TicketResponse();
        response.setToken(JWT);
        response.setUserId(USER_ID);
        response.setApplicationName("IZUDFLT");
        response.setTicket(PASSTICKET);

        mockService("zaas").scope(MockService.Scope.CLASS)
            .addEndpoint("/zaas/scheme/ticket")
                .assertion(he -> assertEquals(SERVICE_ID, he.getRequestHeaders().getFirst("X-Service-Id")))
                .assertion(he -> assertEquals(COOKIE_NAME + "=" + JWT, he.getRequestHeaders().getFirst("Cookie")))
                .bodyJson(response)
            .and().start();
    }

    @Nested
    class GivenValidAuthentication {

        @Test
        void whenRequestingPassticketForAllowedAPPLID_thenTranslate() throws IOException {
            String expectedAuthHeader = "Basic " + Base64.getEncoder().encodeToString((USER_ID + ":" + PASSTICKET).getBytes(StandardCharsets.UTF_8));
            var mockService = mockService(SERVICE_ID)
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

    }

}
