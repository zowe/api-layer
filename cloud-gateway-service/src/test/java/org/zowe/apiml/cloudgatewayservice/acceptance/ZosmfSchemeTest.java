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

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opentest4j.AssertionFailedError;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTest;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MockService;
import org.zowe.apiml.zaas.zosmf.ZosmfResponse;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ZosmfSchemeTest extends AcceptanceTestWithMockServices {

    private static final String COOKIE_NAME = "zosmf_cookie";
    private static final String JWT = "jwt";
    private static final ZosmfResponse OK_RESPONSE = new ZosmfResponse(COOKIE_NAME, JWT);

    private MockService zaasError2;
    private MockService zaasError;
    private MockService zaasOk;

    private MockService service;

    private String getServiceUrl() {
        return basePath + "/service/api/v1/test";
    }

    private String getCookie(HttpExchange httpExchange, String cookieName) {
        List<HttpCookie> cookies = httpExchange.getRequestHeaders().get("Cookie").stream()
            .map(HttpCookie::parse)
            .flatMap(Collection::stream)
            .filter(c -> StringUtils.equalsIgnoreCase(cookieName, c.getName()))
            .collect(Collectors.toList());
        assertTrue(cookies.size() <= 1);
        return cookies.isEmpty() ? null : cookies.get(0).getValue();
    }

    @BeforeAll
    void createAllZaasServices() throws IOException {
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

        // south-bound service - alive for all tests
        service = mockService("service").scope(MockService.Scope.CLASS)
            .authenticationScheme(AuthenticationScheme.ZOSMF)
            .addEndpoint("/service/test")
                .assertion(he -> assertEquals(JWT,
                    he.getRequestHeaders().get(HttpHeaders.COOKIE).stream()
                        .map(HttpCookie::parse)
                        .flatMap(Collection::stream)
                        .filter(c -> COOKIE_NAME.equals(c.getName()))
                        .findFirst().orElseThrow(() -> new AssertionFailedError("No z/OSMF token is set")).getValue()
                ))
            .and().start();
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

    @Test
    void givenMultipleHeaders_whenCallingAService_thenTheyAreResend() throws IOException {
        zaasError2.stop();
        zaasError.stop();
        zaasOk.stop();

        mockService("gateway")
            .addEndpoint("/gateway/zaas/zosmf")
                .bodyJson(OK_RESPONSE)
                .assertion(he -> assertEquals("Bearer userJwt", he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))

                .assertion(he -> assertEquals("service", he.getRequestHeaders().getFirst("x-service-id")))

                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("myheader")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-SAF-Token")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-Public")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-DistinguishedName")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-CommonName")))

                .assertion(he -> assertNull(getCookie(he, "mycookie")))
                .assertion(he -> assertEquals("pat", getCookie(he, "personalAccessToken")))
                .assertion(he -> assertEquals("jwt1", getCookie(he, "apimlAuthenticationToken")))
                .assertion(he -> assertEquals("jwt2", getCookie(he, "apimlAuthenticationToken.2")))
                .assertion(he -> assertNull(getCookie(he, "jwtToken")))
                .assertion(he -> assertNull(getCookie(he, "LtpaToken2")))
            .and().start();

        given()
            .header(HttpHeaders.AUTHORIZATION, "Bearer userJwt")

            .header("myheader", "myvalue")
            .header("X-SAF-Token", "X-SAF-Token")
            .header("X-Certificate-Public", "X-Certificate-Public")
            .header("X-Certificate-DistinguishedName", "X-Certificate-DistinguishedName")
            .header("X-Certificate-CommonName", "X-Certificate-CommonName")

            .cookie("mycookie", "mycookievalue")
            .cookie("personalAccessToken", "pat")
            .cookie("apimlAuthenticationToken", "jwt1")
            .cookie("apimlAuthenticationToken.2", "jwt2")
            .cookie("jwtToken", "jwtToken")
            .cookie("LtpaToken2", "LtpaToken2")
        .when().get(getServiceUrl()).then().statusCode(200);
    }

}
