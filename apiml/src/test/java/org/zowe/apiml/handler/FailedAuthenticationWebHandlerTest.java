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
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FailedAuthenticationWebHandlerTest {

    private AuthExceptionHandler authExceptionHandler;
    private FailedAuthenticationWebHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        authExceptionHandler = mock(AuthExceptionHandler.class);
        handler = new FailedAuthenticationWebHandler(objectMapper, authExceptionHandler);
    }

    @Test
    void onAuthenticationFailure_shouldWriteErrorMessage() throws Exception {
        var request = MockServerHttpRequest.get("/secure").build();
        var exchange = MockServerWebExchange.from(request);
        var response = exchange.getResponse();
        var webFilterExchange = new WebFilterExchange(exchange, mock(WebFilterChain.class));

        var exception = new BadCredentialsException("Invalid credentials");

        var apiMessage = new ApiMessage("org.zowe.apiml.zaas.keys.wrongAmount", MessageType.ERROR, "ZWEAG715E", "cnt", null, null);
        var view = new ApiMessageView(List.of(apiMessage));

        doAnswer(invocation -> {
            BiConsumer<ApiMessageView, HttpStatus> consumer = invocation.getArgument(1);
            consumer.accept(view, HttpStatus.UNAUTHORIZED);
            return null;
        }).when(authExceptionHandler).handleException(any(), any(), any(), eq(exception));

        Mono<Void> result = handler.onAuthenticationFailure(webFilterExchange, exception);

        StepVerifier.create(result).verifyComplete();

        Mono<String> responseBodyMono = DataBufferUtils.join(response.getBody())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return new String(bytes, StandardCharsets.UTF_8);
            });

        StepVerifier.create(responseBodyMono)
            .expectNextMatches(body -> body.contains("ZWEAG715E"))
            .verifyComplete();

        assertEquals("application/json", Objects.requireNonNull(response.getHeaders().getContentType()).toString());
        assertEquals(401, Objects.requireNonNull(response.getStatusCode()).value());
    }

    @Test
    void onAuthenticationFailure_whenSerializationFails_shouldWriteEmptyBody() throws Exception {
        var objectMapperMock = mock(ObjectMapper.class);
        handler = new FailedAuthenticationWebHandler(objectMapperMock, authExceptionHandler);

        var request = MockServerHttpRequest.get("/secure").build();
        var exchange = MockServerWebExchange.from(request);
        var response = exchange.getResponse();
        WebFilterChain mockChain = mock(WebFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());
        var webFilterExchange = new WebFilterExchange(exchange, mockChain);

        var exception = new BadCredentialsException("fail");

        var view = new ApiMessageView(List.of(new ApiMessage("org.zowe.apiml.zaas.keys.wrongAmount", MessageType.ERROR, "ZWEAG715E", "cnt", null, null)));

        doAnswer(invocation -> {
            BiConsumer<ApiMessageView, HttpStatus> consumer = invocation.getArgument(1);
            consumer.accept(view, HttpStatus.UNAUTHORIZED);
            return null;
        }).when(authExceptionHandler).handleException(any(), any(), any(), eq(exception));

        when(objectMapperMock.writeValueAsBytes(any())).thenThrow(new JsonProcessingException("fail") {});

        StepVerifier.create(handler.onAuthenticationFailure(webFilterExchange, exception))
            .verifyComplete();

        DataBuffer buffer = response.getBody().blockFirst();
        assertNull(buffer, "Expected no DataBuffer to be written due to serialization failure");
        assertEquals(401, Objects.requireNonNull(response.getStatusCode()).value());
    }
}
