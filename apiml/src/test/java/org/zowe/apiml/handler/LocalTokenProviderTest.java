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
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalTokenProviderTest {

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
        String token = "valid-token";
        TokenAuthentication mockAuth = mock(TokenAuthentication.class);
        when(mockAuth.getPrincipal()).thenReturn("user123");

        when(authenticationService.validateJwtToken(token)).thenReturn(mockAuth);

        Mono<QueryResponse> result = tokenProvider.validateToken(token);

        StepVerifier.create(result)
            .assertNext(response -> {
                assert response != null;
                assert "user123".equals(response.getUserId());
            })
            .verifyComplete();
    }

    @Test
    void validateToken_invalidToken_returnsEmptyQueryResponse() {
        String token = "invalid-token";

        when(authenticationService.validateJwtToken(token))
            .thenThrow(new RuntimeException("Invalid token"));

        Mono<QueryResponse> result = tokenProvider.validateToken(token);

        StepVerifier.create(result)
            .assertNext(response -> {
                assert response != null;
                assert response.getUserId() == null;
            })
            .verifyComplete();
    }
}
