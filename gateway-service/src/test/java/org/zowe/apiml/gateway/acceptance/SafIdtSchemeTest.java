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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MockService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SafIdtSchemeTest {

    private static final String SERVICE_ID = "service";
    private static final String SAF_IDT = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJVU0VSIiwiZXhwIjoxNzAxMjc2NTUyfQ.";
    private static final ObjectWriter WRITER = new ObjectMapper().writer();


    private String getHeaderValue(HttpExchange httpExchange, String headerName) {
        List<String> headerValue = Optional.ofNullable(httpExchange.getRequestHeaders().get(headerName))
            .orElse(Collections.emptyList());
        assertTrue(headerValue.size() <= 1);
        return headerValue.isEmpty() ? null : headerValue.get(0);
    }

    @Nested
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GivenValidAuth extends AcceptanceTestWithMockServices {
        MockService zaas;
        MockService service;

        @BeforeEach
        void setup() throws IOException {
            ZaasTokenResponse response = new ZaasTokenResponse();
            response.setToken(SAF_IDT);

            zaas = mockService("zaas").scope(MockService.Scope.TEST)
                .addEndpoint("/zaas/zaas/safIdt")
                .responseCode(200)
                .assertion(he -> assertEquals("Bearer userJwt", he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .assertion(he -> {
                    try {
                        assertEquals(WRITER.writeValueAsString(new TicketRequest("IZUDFLT")), IOUtils.toString(he.getRequestBody(), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .bodyJson(response)
                .and().start();
            service = mockService(SERVICE_ID).scope(MockService.Scope.TEST)
                .authenticationScheme(AuthenticationScheme.SAF_IDT).applid("IZUDFLT")
                .addEndpoint("/" + SERVICE_ID + "/test")
                .assertion(he -> assertEquals(SAF_IDT, getHeaderValue(he, "x-saf-token")))
                .and().start();
        }

        @Test
        void thenReturnSAFIDtoken() {
            given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer userJwt")
                .when()
                .get(basePath + "/" + SERVICE_ID + "/api/v1/test")
                .then()
                .statusCode(Matchers.is(SC_OK));
        }
    }

    @Nested
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class GivenNoAuth extends AcceptanceTestWithMockServices {
        MockService zaas;
        MockService service;

        @BeforeEach
        void setup() throws IOException {

            zaas = mockService("zaas").scope(MockService.Scope.CLASS)
                .addEndpoint("/zaas/zaas/safIdt")
                .responseCode(401)
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                .and().start();
            service = mockService(SERVICE_ID).scope(MockService.Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.SAF_IDT).applid("IZUDFLT")
                .addEndpoint("/" + SERVICE_ID + "/test")
                .assertion(he -> assertNull(getHeaderValue(he, "x-saf-token")))
                .assertion(he -> assertEquals("Invalid or missing authentication", getHeaderValue(he, ApimlConstants.AUTH_FAIL_HEADER)))
                .and().start();
        }

        @Test
        void thenReturnError() {

            given()
                .when()
                .get(basePath + "/" + SERVICE_ID + "/api/v1/test")
                .then()
                .statusCode(Matchers.is(SC_OK));
        }
    }

}
