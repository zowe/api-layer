/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenControllerTest {

    private static final String VALID_USER = "user";
    private static final char[] VALID_PASSWORD = "password".toCharArray();
    private static final String INVALID_USER = "invalidUser";
    private static final char[] INVALID_PASSWORD = "invalidPassword".toCharArray();
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";
    private static final String VALID_CREDENTIALS_BASE64 = Base64.getEncoder().encodeToString(
        (VALID_USER + ":" + String.valueOf(VALID_PASSWORD)).getBytes()
    );
    private static final String INVALID_CREDENTIALS_BASE64 = Base64.getEncoder().encodeToString(
        (INVALID_USER + ":" + String.valueOf(INVALID_PASSWORD)).getBytes()
    );

    @MockitoBean
    private GatewaySecurityService gatewaySecurityService;

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    class Login {

        @BeforeEach
        void mockGatewayClient() {
            doReturn(Optional.of(VALID_TOKEN)).when(gatewaySecurityService).login(VALID_USER, VALID_PASSWORD, null);
            doReturn(Optional.empty()).when(gatewaySecurityService).login(INVALID_USER, INVALID_PASSWORD, null);
        }

        @Test
        void givenValidCredentialsInHeader_whenLogin_thenSuccess() {
            webTestClient.post()
                .uri("/apicatalog/auth/login")
                .header(AUTHORIZATION, "Basic " + VALID_CREDENTIALS_BASE64)
                .exchange()
                .expectStatus().isNoContent()
                .expectCookie().value(COOKIE_AUTH_NAME, VALID_TOKEN::equals);
        }

        @Test
        void givenValidCredentialsInBody_whenLogin_thenSuccess() {
            webTestClient.post()
                .uri("/apicatalog/auth/login")
                .bodyValue(new LoginRequest(VALID_USER, VALID_PASSWORD))
                .exchange()
                .expectStatus().isNoContent()
                .expectCookie().value(COOKIE_AUTH_NAME, VALID_TOKEN::equals);
        }

        @Test
        void givenInvalidCredentialsInHeader_whenLogin_thenRejected() {
            webTestClient.post()
                .uri("/apicatalog/auth/login")
                .header(AUTHORIZATION, "Basic " + INVALID_CREDENTIALS_BASE64)
                .exchange()
                .expectStatus().isEqualTo(SC_UNAUTHORIZED);
        }

        @Test
        void givenInvalidCredentialsInBody_whenLogin_thenRejected() {
            webTestClient.post()
                .uri("/apicatalog/auth/login")
                .bodyValue(new LoginRequest(INVALID_USER, INVALID_PASSWORD))
                .exchange()
                .expectStatus().isEqualTo(SC_UNAUTHORIZED);
        }

        @Test
        void givenNoCredentials_whenLogin_thenBadRequest() {
            webTestClient.post()
                .uri("/apicatalog/auth/login")
                .exchange()
                .expectStatus().isBadRequest();
        }

    }

    @Nested
    class Query {

        @BeforeEach
        void mockGatewayClient() {
            doReturn(new QueryResponse("domain", "user", new Date(), new Date(), "issuer", Collections.singletonList("scope"), null))
                .when(gatewaySecurityService).query(VALID_TOKEN);
            doThrow(new BadCredentialsException("unauthorized")).when(gatewaySecurityService).query(INVALID_TOKEN);
        }

        @Test
        void givenValidCredentialsInHeader_whenQuery_thenSuccess() {
            webTestClient.get()
                .uri("/apicatalog/auth/query")
                .header(AUTHORIZATION, "Bearer " + VALID_TOKEN)
                .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                        .jsonPath("userId").value("user"::equals);
        }

        @Test
        void givenValidCredentialsInCookie_whenQuery_thenSuccess() {
            webTestClient.get()
                .uri("/apicatalog/auth/query")
                .cookie(COOKIE_AUTH_NAME, VALID_TOKEN)
                .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                        .jsonPath("userId").value("user"::equals);
        }

        @Test
        void givenInvalidCredentialsInHeader_whenQuery_thenReject() {
            webTestClient.get()
                .uri("/apicatalog/auth/query")
                .header(AUTHORIZATION, "Bearer " + INVALID_TOKEN)
                .exchange()
                    .expectStatus().isEqualTo(SC_UNAUTHORIZED);
        }

        @Test
        void givenInvalidCredentialsInCookie_whenQuery_thenReject() {
            webTestClient.get()
                .uri("/apicatalog/auth/query")
                .cookie(COOKIE_AUTH_NAME, INVALID_TOKEN)
                .exchange()
                    .expectStatus().isEqualTo(SC_UNAUTHORIZED);
        }

        @Test
        void givenNoCredentialsInCookie_whenQuery_thenReject() {
            webTestClient.get()
                .uri("/apicatalog/auth/query")
                .exchange()
                    .expectStatus().isEqualTo(SC_UNAUTHORIZED);
        }

    }

}
