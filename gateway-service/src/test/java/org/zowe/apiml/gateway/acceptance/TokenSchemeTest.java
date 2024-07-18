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

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MockService;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.*;

public abstract class TokenSchemeTest {

    private static final String COOKIE_NAME = "token_cookie";
    private static final String JWT = "jwt";
    private static final ZaasTokenResponse OK_RESPONSE = ZaasTokenResponse.builder().cookieName(COOKIE_NAME).token(JWT).build();

    public abstract String getTokenEndpoint();

    public abstract AuthenticationScheme getAuthenticationScheme();

    private String getCookie(HttpExchange httpExchange, String cookieName) {
        List<String> cookieList = httpExchange.getRequestHeaders().get("Cookie");
        if (cookieList == null || cookieList.isEmpty()) return null;
        var allCookies = new ArrayList<String>();
        for (String cookies : cookieList) {
            allCookies.addAll(Arrays.asList(cookies.split(";")));
        }
        List<HttpCookie> cookies = allCookies.stream()
            .map(HttpCookie::parse)
            .flatMap(Collection::stream)
            .filter(c -> StringUtils.equalsIgnoreCase(cookieName, c.getName()))
            .toList();
        assertTrue(cookies.size() <= 1);
        return cookies.isEmpty() ? null : cookies.get(0).getValue();
    }

    @Nested
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class HighAvailability extends AcceptanceTestWithMockServices {

        private MockService zaasZombie;
        private MockService zaasError;
        private MockService zaasOk;

        private MockService service;

        private String getServiceUrl() {
            return basePath + "/service/api/v1/test";
        }

        @BeforeAll
        void createAllZaasServices() throws IOException {
            // on the beginning prepare all as zombie, each test will decide
            zaasError = mockService("zaas").scope(MockService.Scope.CLASS)
                .addEndpoint(getTokenEndpoint())
                .responseCode(500)
                .and().build();
            zaasZombie = mockService("zaas").scope(MockService.Scope.CLASS)
                .addEndpoint(getTokenEndpoint())
                .bodyJson(new ZaasTokenResponse())
                .and().build();
            zaasOk = mockService("zaas").scope(MockService.Scope.CLASS)
                .addEndpoint(getTokenEndpoint())
                .bodyJson(OK_RESPONSE)
                .assertion(he -> assertEquals("service", he.getRequestHeaders().getFirst("x-service-id")))
                .and().build();

            // south-bound service - alive for all tests
            service = mockService("service").scope(MockService.Scope.CLASS)
                .authenticationScheme(getAuthenticationScheme())
                .addEndpoint("/service/test")
                .assertion(he -> assertEquals(JWT, getCookie(he, COOKIE_NAME)))
                .and().start();
        }

        @Test
        void givenNoInstanceOfZosmf_whenCallingAService_thenReturn500() {
            zaasZombie.stop();
            zaasError.stop();
            zaasOk.stop();

            given().when().get(getServiceUrl()).then().statusCode(500);
            assertEquals(0, service.getCounter());
        }

        @Test
        void givenInstanceOfZosmf_whenCallingAService_thenReturn200() throws IOException {
            zaasZombie.stop();
            zaasError.stop();
            zaasOk.start();

            given().when().get(getServiceUrl()).then().statusCode(200);
            assertEquals(1, service.getCounter());
        }

        @Test
        void givenZombieAndOkInstanceOfZosmf_whenCallingAService_preventZombieOne() throws IOException {
            zaasZombie.zombie();
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
            zaasZombie.zombie();
            zaasError.stop();
            zaasOk.stop();

            given().when().get(getServiceUrl()).then().statusCode(500);
            assertEquals(0, service.getCounter());
        }

        @Test
        void givenZombieAndErrorZosmf_whenCallingAService_return500() throws IOException {
            zaasZombie.zombie();
            zaasError.start();
            zaasOk.stop();

            given().when().get(getServiceUrl()).then().statusCode(500);
            assertEquals(0, service.getCounter());
        }

        @Test
        void givenZombieFailingAndSuccessZosmf_whenCallingAService_return200() throws IOException {
            zaasZombie.zombie();
            zaasError.start();
            zaasOk.start();

            for (int i = 1; i < 10; i++) {
                given().when().get(getServiceUrl()).then().statusCode(200);
                assertEquals(i, zaasOk.getCounter());
                assertEquals(i, service.getCounter());
            }
            assertNotEquals(0, zaasError.getCounter());
        }

    }

    @Nested
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class InvalidAuthentication extends AcceptanceTestWithMockServices {

        private MockService zaas;
        private MockService service;

        private String getServiceUrl() {
            return basePath + "/service/api/v1/test";
        }

        @BeforeAll
        void init() throws IOException {
            zaas = mockService("zaas").scope(MockService.Scope.CLASS)
                .addEndpoint(getTokenEndpoint())
                .responseCode(401)
                .and().start();

            service = mockService("service").scope(MockService.Scope.CLASS)
                .authenticationScheme(getAuthenticationScheme())
                .addEndpoint("/service/test")
                .assertion(he -> assertNull(getCookie(he, COOKIE_NAME)))
                .and().start();
        }

        @Test
        void givenZaasWithInvalidResponse_whenCallingAService_thenDontPropagateCredentials() {
            given().when().get(getServiceUrl()).then().statusCode(200);
            assertEquals(1, zaas.getCounter());
            assertEquals(1, service.getCounter());
        }

    }

    @Nested
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ResponseWithoutToken extends AcceptanceTestWithMockServices {

        private MockService zaas;
        private MockService service;

        private String getServiceUrl() {
            return basePath + "/service/api/v1/test";
        }

        @BeforeAll
        void init() throws IOException {
            zaas = mockService("zaas").scope(MockService.Scope.CLASS)
                .addEndpoint(getTokenEndpoint())
                .responseCode(200)
                .bodyJson(new ZaasTokenResponse())
                .and().start();


            service = mockService("service").scope(MockService.Scope.CLASS)
                .authenticationScheme(getAuthenticationScheme())
                .addEndpoint("/service/test")
                .assertion(he -> assertNull(getCookie(he, COOKIE_NAME)))
                .and().start();
        }

        @Test
        void givenNoCredentials_whenCallingAService_thenDontPropagateCredentials() {
            given().when().get(getServiceUrl()).then().statusCode(200);
            assertEquals(1, zaas.getCounter());
            assertEquals(1, service.getCounter());
        }

        @Test
        void givenInvalidCredentials_whenCallingAService_thenDontPropagateCredentials() {
            given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer nonSense")
                .when()
                .get(getServiceUrl())
                .then()
                .statusCode(200);
            assertEquals(1, zaas.getCounter());
            assertEquals(1, service.getCounter());
        }

    }

    @Nested
    @AcceptanceTest
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class ZaasCommunication extends AcceptanceTestWithMockServices {

        private MockService service;

        @BeforeAll
        void initService() throws IOException {
            service = createService();
        }

        MockService createZaasSuccess() throws IOException {
            return mockService("zaas").scope(MockService.Scope.TEST)
                .addEndpoint(getTokenEndpoint())
                .responseCode(SC_OK)
                .bodyJson(OK_RESPONSE)
                .assertion(he -> assertEquals("Bearer userJwt", he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))

                .assertion(he -> assertEquals("service", he.getRequestHeaders().getFirst("x-service-id")))

                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("myheader")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-SAF-Token")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-Public")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-DistinguishedName")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-CommonName")))
                .assertion(he -> assertNull(he.getRequestHeaders().getFirst("Client-Cert")))

                .assertion(he -> assertNull(getCookie(he, "mycookie")))
                .assertion(he -> assertEquals("pat", getCookie(he, "personalAccessToken")))
                .assertion(he -> assertEquals("jwt1", getCookie(he, "apimlAuthenticationToken")))
                .assertion(he -> assertEquals("jwt2", getCookie(he, "apimlAuthenticationToken.2")))
                .assertion(he -> assertNull(getCookie(he, "jwtToken")))
                .assertion(he -> assertNull(getCookie(he, "LtpaToken2")))
                .and().start();
        }

        MockService createZaasFailure() throws IOException {
            return mockService("zaas").scope(MockService.Scope.TEST)
                .addEndpoint(getTokenEndpoint())
                .responseCode(SC_UNAUTHORIZED)
                .and().start();
        }

        MockService createService() throws IOException {
            return mockService("service").scope(MockService.Scope.CLASS)
                .authenticationScheme(getAuthenticationScheme())
                .addEndpoint("/service/test/success")
                    .assertion(he -> assertEquals(JWT, getCookie(he, COOKIE_NAME)))

                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("x-service-id")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-SAF-Token")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-Public")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-DistinguishedName")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-CommonName")))
                    .assertion(he -> assertEquals("myvalue", he.getRequestHeaders().getFirst("myheader")))

                    .assertion(he -> assertNull(getCookie(he, "personalAccessToken")))
                    .assertion(he -> assertNull(getCookie(he, "apimlAuthenticationToken")))
                    .assertion(he -> assertNull(getCookie(he, "apimlAuthenticationToken.2")))
                    .assertion(he -> assertNull(getCookie(he, "jwtToken")))
                    .assertion(he -> assertNull(getCookie(he, "LtpaToken2")))
                    .assertion(he -> assertEquals("mycookievalue", getCookie(he, "mycookie")))
                .and()

                .addEndpoint("/service/test/fail")
                    .assertion(he -> assertNull(getCookie(he, COOKIE_NAME)))

                    .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("x-service-id")))
                    .assertion(he -> assertNotNull(he.getRequestHeaders().getFirst("X-SAF-Token")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-Public")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-DistinguishedName")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-CommonName")))
                    .assertion(he -> assertEquals("myvalue", he.getRequestHeaders().getFirst("myheader")))

                    .assertion(he -> assertNotNull(getCookie(he, "personalAccessToken")))
                    .assertion(he -> assertNotNull(getCookie(he, "apimlAuthenticationToken")))
                    .assertion(he -> assertNotNull(getCookie(he, "apimlAuthenticationToken.2")))
                    .assertion(he -> assertNotNull(getCookie(he, "jwtToken")))
                    .assertion(he -> assertNotNull(getCookie(he, "LtpaToken2")))
                    .assertion(he -> assertEquals("mycookievalue", getCookie(he, "mycookie")))
                .and().start();
        }

        private String getServiceUrl(String path) {
            return basePath + "/service/api/v1/test/" + path;
        }

        private void makeACall(String path) {
            given()
                .header(HttpHeaders.AUTHORIZATION, "Bearer userJwt")

                .header("myheader", "myvalue")
                .header("X-SAF-Token", "X-SAF-Token")
                .header("X-Certificate-Public", "X-Certificate-Public")
                .header("X-Certificate-DistinguishedName", "X-Certificate-DistinguishedName")
                .header("X-Certificate-CommonName", "X-Certificate-CommonName")
                .header("Client-Cert", "certData")

                .cookie("mycookie", "mycookievalue")
                .cookie("personalAccessToken", "pat")
                .cookie("apimlAuthenticationToken", "jwt1")
                .cookie("apimlAuthenticationToken.2", "jwt2")
                .cookie("jwtToken", "jwtToken")
                .cookie("LtpaToken2", "LtpaToken2")
            .when()
                .get(getServiceUrl(path))
            .then()
                .statusCode(200);
        }

        @Test
        void givenMultipleHeaders_whenCallingAService_thenTheyAreResend() throws IOException {
            try (MockService zaas = createZaasSuccess()) {
                makeACall("success");

                assertEquals(1, zaas.getCounter());
                assertEquals(1, service.getCounter());
            }
        }

        @Test
        void givenInvalidCredentials_whenCallingAService_thenResendAllHeadersExceptClientCertificate() throws IOException {
            try (MockService zaas = createZaasFailure()) {
                makeACall("fail");

                assertEquals(1, zaas.getCounter());
                assertEquals(1, service.getCounter());
            }
        }

    }

}
