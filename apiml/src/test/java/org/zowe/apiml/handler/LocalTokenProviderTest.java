/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.util.JWTTestUtils;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalTokenProviderTest {

    public static final String USERNAME = "user123";
    public static final String VALID_TOKEN = JWTTestUtils.createDummyAPIMLToken(USERNAME);

    private AuthenticationService authenticationService;
    private LocalTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        WebClient webClient = mock(WebClient.class);
        InstanceInfoService instanceInfoService = mock(InstanceInfoService.class);
        authenticationService = mock(AuthenticationService.class);
        tokenProvider = new LocalTokenProvider(webClient, instanceInfoService, authenticationService);
    }

    @Test
    void validateToken_validToken_returnsPrincipal() {
        TokenAuthentication mockAuth = mock(TokenAuthentication.class);

        when(authenticationService.validateJwtToken(VALID_TOKEN)).thenReturn(mockAuth);
        when(authenticationService.parseJwtToken(VALID_TOKEN)).thenReturn(new TokenAuthentication(VALID_TOKEN));

        Mono<QueryResponse> result = tokenProvider.validateToken(VALID_TOKEN);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertNotNull(response);
                assertEquals(USERNAME, response.getUserId());
            })
            .verifyComplete();
    }

    @Test
    void validateToken_invalidToken_throwAuthException() {
        String token = "invalid-token";

        when(authenticationService.validateJwtToken(token))
            .thenThrow(new RuntimeException("Invalid token"));

        Mono<QueryResponse> result = tokenProvider.validateToken(token);

        StepVerifier.create(result)
            .expectError(AuthenticationCredentialsNotFoundException.class)
            .verify();
    }
}
