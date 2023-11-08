/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MockService;
import org.zowe.apiml.zaas.zosmf.ZosmfResponse;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZosmfSchemeTest  extends AcceptanceTestWithMockServices {

    private static final String COOKIE_NAME = "zosmf_cookie";
    private static final String JWT = "jwt";
    private static ZosmfResponse OK_RESPONSE = new ZosmfResponse(COOKIE_NAME, JWT);

    private MockService zaasError2;
    private MockService zaasError;
    private MockService zaasOk;

    private MockService service;

    @BeforeAll
    void createAllMockServices() throws IOException {
        // on the beginning prepare all as zombie, each test will decide
        zaasError = mockService("gateway").scope(MockService.Scope.CLASS)
            .addEndpoint("/gateway/zaas/zosmf")
            .responseCode(500)
            .and().build();
        zaasError2 = mockService("gateway").scope(MockService.Scope.CLASS)
            .addEndpoint("/gateway/zaas/zosmf")
                .bodyJson(new ZosmfResponse())
            .and().build();
        zaasOk = mockService("gateway").scope(MockService.Scope.CLASS)
            .addEndpoint("/gateway/zaas/zosmf")
                .bodyJson(OK_RESPONSE)
                .assertion(he -> assertEquals("service", he.getRequestHeaders().getFirst("x-service-id")))
            .and().build();

        // south-bound service
        service = mockService("service").scope(MockService.Scope.CLASS)
            .authenticationScheme(AuthenticationScheme.ZOSMF)
            .addEndpoint("/service/test")
                .assertion(he -> assertEquals(COOKIE_NAME + "=\"" + JWT + "\"", he.getRequestHeaders().getFirst("Cookie")))
            .and().start();
    }

    private String getServiceUrl() {
        return basePath + "/service/api/v1/test";
    }

    @Test
    void givenNoInstanceOfZosmf_whenCallingAService_thenReturn500() {
        zaasError2.stop();
        zaasError.stop();
        zaasOk.stop();

        given().when().get(getServiceUrl()).then().statusCode(500);
        assertEquals(0, service.getCounter());
    }

    @Test
    void givenInstanceOfZosmf_whenCallingAService_thenReturn200() throws IOException {
        zaasError2.stop();
        zaasError.stop();
        zaasOk.start();

        given().when().get(getServiceUrl()).then().statusCode(200);
        assertEquals(1, service.getCounter());
    }

    @Test
    void givenZombieAndOkInstanceOfZosmf_whenCallingAService_preventZombieOne() throws IOException {
        zaasError2.zombie();
        zaasError.stop();
        zaasOk.start();

        for (int i = 1; i < 10; i++) {
            given().when().get(getServiceUrl()).then().statusCode(200);
            assertEquals(i, zaasOk.getCounter());
            assertEquals(i, service.getCounter());
        }
    }

    @Test
    void givenOnlyZombieZosmf_whenCallingAService_return500() {
        zaasError2.zombie();
        zaasError.stop();
        zaasOk.stop();

        given().when().get(getServiceUrl()).then().statusCode(500);
        assertEquals(0, service.getCounter());
    }

    @Test
    void givenZombieAndErrorZosmf_whenCallingAService_return500() throws IOException {
        zaasError2.zombie();
        zaasError.start();
        zaasOk.stop();

        given().when().get(getServiceUrl()).then().statusCode(500);
        assertEquals(0, service.getCounter());
    }

    @Test
    void givenZombieFailingAndSuccessZosmf_whenCallingAService_return200() throws IOException {
        zaasError2.zombie();
        zaasError.start();
        zaasOk.start();

        for (int i = 1; i < 10; i++) {
            given().when().get(getServiceUrl()).then().statusCode(200);
            assertEquals(i, zaasOk.getCounter());
            assertEquals(i, service.getCounter());
        }
        assertNotEquals(0, zaasError.getCounter());
    }

    @Test
    void givenZaasWithInvalidResponse_whenCallingAService_return500() throws IOException {
        zaasError2.start();
        zaasError.stop();
        zaasOk.stop();

        given().when().get(getServiceUrl()).then().statusCode(500);
        assertEquals(1, zaasError2.getCounter());
        assertEquals(0, service.getCounter());
    }

}
