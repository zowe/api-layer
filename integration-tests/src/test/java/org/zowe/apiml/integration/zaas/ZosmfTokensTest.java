/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.zaas;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaas.ZosmfTokensResponse;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@Slf4j
@GeneralAuthenticationTest
class ZosmfTokensTest implements TestWithStartedInstances {

    private final static String COOKIE = "apimlAuthenticationToken";
    private final URI zosmfTokens_URL = HttpRequestUtils.getUriFromGateway("/gateway/zaas/zosmf");

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class WhenGeneratingZosmfTokens {
        private String jwt;

        @BeforeEach
        void setUpToken() {
            jwt = gatewayToken();
        }

        @Nested
        class ReturnValidZosmfToken {
            @BeforeEach
            void setUpCertificate() {
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
            }

            @Test
            void givenValidTokenInCookieAndCertificate() {
                ZosmfTokensResponse zosmfResponse = given()
                    .cookie(COOKIE, jwt)
                .when()
                    .post(zosmfTokens_URL)
                .then()
                    .statusCode(is(SC_OK))
                    .extract().body().as(ZosmfTokensResponse.class);

                assertNotNull(zosmfResponse.getJwtToken());
            }

            @Test
            void givenValidTokenInHeaderAndCertificate() {
                ZosmfTokensResponse zosmfResponse = given()
                    .header("Authorization", "Bearer " + jwt)
                .when()
                    .post(zosmfTokens_URL)
                .then()
                    .statusCode(is(SC_OK))
                    .extract().body().as(ZosmfTokensResponse.class);

                assertNotNull(zosmfResponse.getJwtToken());
            }
        }

        @Nested
        class ReturnUnauthorized {
            @BeforeEach
            void setUpCertificateAndToken() {
                RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
            }

            @Test
            void givenNoToken() {
                String expectedMessage = "Authentication is required for URL '" + zosmfTokens_URL.getPath() + "'";

                given()
                .when()
                    .post(zosmfTokens_URL)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG105E' }.messageContent", equalTo(expectedMessage));
            }

            @Test
            void givenInvalidTokenInCookie() {
                String jwt = "invalidToken";
                String expectedMessage = "Token is not valid for URL '" + zosmfTokens_URL.getPath() + "'";

                given()
                    .cookie(COOKIE, jwt)
                .when()
                    .post(zosmfTokens_URL)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
            }

            @Test
            void givenInvalidTokenInHeader() {
                String jwt = "invalidToken";
                String expectedMessage = "Token is not valid for URL '" + zosmfTokens_URL.getPath() + "'";

                given()
                    .header("Authorization", "Bearer " + jwt)
                .when()
                    .post(zosmfTokens_URL)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG130E' }.messageContent", equalTo(expectedMessage));
            }
        }

        @Nested
        class GivenNoCertificate {
            @Test
            void thenReturnUnauthorized() {
                String expectedMessage = "Authentication is required for URL '" + zosmfTokens_URL.getPath() + "'";

                given()
                    .cookie(COOKIE, jwt)
                .when()
                    .post(zosmfTokens_URL)
                .then()
                    .statusCode(is(SC_UNAUTHORIZED))
                    .body("messages.find { it.messageNumber == 'ZWEAG105E' }.messageContent", equalTo(expectedMessage));
            }
        }
    }
}
