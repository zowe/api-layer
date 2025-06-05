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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SuccessQueryHandlerTest {

    private ObjectMapper objectMapper;
    private AuthenticationService authenticationService;
    private SuccessQueryHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authenticationService = mock(AuthenticationService.class);
        handler = new SuccessQueryHandler(objectMapper, authenticationService);
    }

    @Test
    void onAuthenticationSuccess_shouldWriteResponse() throws JsonProcessingException {
        String token = "mockToken";
        TokenAuthentication authentication = new TokenAuthentication("user", token, TokenAuthentication.Type.JWT);
        QueryResponse queryResponse = new QueryResponse();
        queryResponse.setUserId("user");
        byte[] expectedBytes = objectMapper.writeValueAsBytes(queryResponse);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        MockServerHttpResponse response = new MockServerHttpResponse();
        when(exchange.getResponse()).thenReturn(response);

        WebFilterChain chain = mock(WebFilterChain.class);
        WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);

        when(authenticationService.parseJwtToken(token)).thenReturn(queryResponse);

        Mono<Void> result = handler.onAuthenticationSuccess(webFilterExchange, authentication);

        StepVerifier.create(result).verifyComplete();

        DataBuffer buffer = response.getBody().blockFirst();
        assert buffer != null;
        byte[] actualBytes = new byte[buffer.readableByteCount()];
        buffer.read(actualBytes);

        assertEquals(new String(expectedBytes, StandardCharsets.UTF_8), new String(actualBytes, StandardCharsets.UTF_8));
        assertEquals("application/json", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        assertEquals(200, Objects.requireNonNull(response.getStatusCode()).value());
    }
}
