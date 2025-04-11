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

import java.net.URI;

import static io.restassured.RestAssured.given;

@AcceptanceTest
@ActiveProfiles("EurekaEndpointsTests")
public class EurekaEndpointsTests extends AcceptanceTestWithMockServices {

    private static final String USER_ID = "user";
    private static final String COOKIE_NAME = "apimlAuthenticationToken";
    private static final String JWT = "jwt";

    @Test
    void testEurekaHomePage() throws JsonProcessingException {
        var response = ZaasTokenResponse.builder().cookieName(COOKIE_NAME).token(JWT).build();

        mockService("zaas").scope(MockService.Scope.TEST)
            .addEndpoint("/zaas/scheme/zosmf")
            .responseCode(201)
            .bodyJson(response)
            .and().start();

        given()
        .when()
            .auth().preemptive().basic(USER_ID, "user")
            .get(URI.create("https://localhost:10011"))
        .then()
            .statusCode(200);
    }

}
