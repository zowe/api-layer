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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.login.LoginRequest;

import java.util.Base64;
import java.util.Optional;

import static org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
import static org.mockito.Mockito.doReturn;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@AutoConfigureWebTestClient(timeout = "3600000")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenControllerTest {

    private static final String VALID_USER = "user";
    private static final char[] VALID_PASSWORD = "password".toCharArray();
    private static final String INVALID_USER = "invalidUser";
    private static final char[] INVALID_PASSWORD = "invalidPassword".toCharArray();
    private static final String VALID_TOKEN = "valid.jwt.token";
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
            .uri("/apicatalog/api/v1/auth/login")
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
