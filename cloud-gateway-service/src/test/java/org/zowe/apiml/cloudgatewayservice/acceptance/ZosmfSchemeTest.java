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

import org.zowe.apiml.auth.AuthenticationScheme;

public class ZosmfSchemeTest extends TokenSchemeTest {

    public String getTokenEndpoint() {
        return "/gateway/zaas/zosmf";
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
        void createZaas() throws IOException {
            zaas = mockService("gateway").scope(MockService.Scope.CLASS)
                .addEndpoint("/gateway/zaas/zosmf")
                    .responseCode(200)
                    .bodyJson(new ZaasTokenResponse())
                .and().start();


            service = mockService("service").scope(MockService.Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.ZOSMF)
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
                .header(HttpHeaders.AUTHORIZATION, "Baerer nonSense")
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

        @BeforeAll
        void createZaas() throws IOException {
            mockService("gateway").scope(MockService.Scope.CLASS)
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
        }

        @BeforeAll
        void createService() throws IOException {
            mockService("service").scope(MockService.Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.ZOSMF)
                .addEndpoint("/service/test")
                    .assertion(he -> assertEquals(JWT, getCookie(he, COOKIE_NAME)))

                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION)))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("x-service-id")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-SAF-Token")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-Public")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-DistinguishedName")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("X-Certificate-CommonName")))
                    .assertion(he -> assertNull(he.getRequestHeaders().getFirst("Client-Cert")))
                    .assertion(he -> assertEquals("myvalue", he.getRequestHeaders().getFirst("myheader")))

                    .assertion(he -> assertNull(getCookie(he, "personalAccessToken")))
                    .assertion(he -> assertNull(getCookie(he, "apimlAuthenticationToken")))
                    .assertion(he -> assertNull(getCookie(he, "apimlAuthenticationToken.2")))
                    .assertion(he -> assertNull(getCookie(he, "jwtToken")))
                    .assertion(he -> assertNull(getCookie(he, "LtpaToken2")))
                    .assertion(he -> assertEquals("mycookievalue", getCookie(he, "mycookie")))
                .and().start();
        }

        private String getServiceUrl() {
            return basePath + "/service/api/v1/test";
        }

        @Test
        void givenMultipleHeaders_whenCallingAService_thenTheyAreResend() {
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
                .get(getServiceUrl())
            .then()
                .statusCode(200);
        }

    public AuthenticationScheme getAuthenticationScheme() {
        return AuthenticationScheme.ZOSMF;
    }

}
