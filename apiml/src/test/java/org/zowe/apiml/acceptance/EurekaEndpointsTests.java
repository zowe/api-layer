/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import static io.restassured.RestAssured.given;

@AcceptanceTest
@ActiveProfiles("EurekaEndpointsTests")
public class EurekaEndpointsTests extends AcceptanceTestWithMockServices {

    private static final String USER_ID = "user";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String JWT = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNjcxNDYxNjIzLCJleHAiOjE2NzE0OTA0MjMsImlzcyI6IkFQSU1MIiwianRpIjoiYmFlMTkyZTYtYTYxMi00MThhLWI2ZGMtN2I0NWI5NzM4ODI3IiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIifQ.Vt5UjJUlbmuzmmEIodAACtj_AOxlsWqkFrFyWh4_MQRRPCj_zMIwnzpqRN-NJvKtUg1zxOCzXv2ypYNsglrXc7cH9wU3leK1gjYxK7IJjn2SBEb0dUL5m7-h4tFq2zNhcGH2GOmTpE2gTQGSTvDIdja-TIj_lAvUtbkiorm1RqrNu2MGC0WfgOGiak3tj2tNJLv_Y1ZMxNjzyHgXBMuNPozQrd4Vtnew3x4yy85LrTYF7jJM3U-e3AD2yImftxwycQvbkjNb-lWadejTVH0MgHMr04wVdDd8Nq5q7yrZf7YPzhias8ehNbew5CHiKut9SseZ1sO2WwgfhpEfsN4okg";

    @Test
    void testEurekaHomePage() throws JsonProcessingException {
        var response = ZaasTokenResponse.builder().cookieName(COOKIE_NAME).token(JWT).build();

        mockService("zaas").scope(MockService.Scope.TEST)
            .addEndpoint("/zaas/scheme/zosmf")
            .bodyJson(response)
            .and().start();

        given()
        .when()
            .auth().preemptive().basic(USER_ID, "user")
        .then()
            .statusCode(200);
    }

}
