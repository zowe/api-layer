/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.security;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthExceptionHandlerReactiveTest {

    @Mock private MessageService messageService;
    @Mock private ObjectMapper objectMapper;

    private AuthExceptionHandlerReactive handler;

    @BeforeEach
    void setUp() {
        handler = new AuthExceptionHandlerReactive(messageService, objectMapper);
    }

    @Nested
    class GivenHandler {

        @Mock private ServerWebExchange exchange;

        private MockServerHttpResponse response;

        @BeforeEach
        void setUp() {
            response = new MockServerHttpResponse();
            when(exchange.getResponse()).thenReturn(response);
            var request = mock(ServerHttpRequest.class);
            when(exchange.getRequest()).thenReturn(request);
            when(request.getPath()).thenReturn(mock(RequestPath.class));
        }

        @AfterEach
        void assertResponse() {
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals(List.of("Invalid token"), response.getHeaders().get("X-Zowe-Auth-Failure"));
            assertEquals(List.of("application/json"), response.getHeaders().get("Content-Type"));
        }

        @Test
        void whenHandleInvalidToken_thenUpdateResponse() throws JsonProcessingException {
            var view = mock(ApiMessageView.class);
            var correctMessage = mock(Message.class);
            when(messageService.createMessage(eq("org.zowe.apiml.common.unauthorized"), any(RequestPath.class)))
                .thenReturn(correctMessage);
            when(correctMessage.mapToView()).thenReturn(view);

            when(objectMapper.writeValueAsBytes(view)).thenReturn("{\"code\":\"ZWEA11\"}".getBytes());

            StepVerifier.create(handler.handleTokenNotValid(exchange))
                .expectComplete()
                .verify();

            assertEquals("{\"code\":\"ZWEA11\"}", response.getBodyAsString().block());
        }

        @Test
        void whenHandleInvalidToken_AndException_thenDefaultMessage() throws JsonProcessingException {
            var view = mock(ApiMessageView.class);
            var correctMessage = mock(Message.class);
            when(messageService.createMessage(eq("org.zowe.apiml.common.unauthorized"), any(RequestPath.class)))
                .thenReturn(correctMessage);
            when(correctMessage.mapToView()).thenReturn(view);

            when(objectMapper.writeValueAsBytes(view)).thenThrow(new JsonParseException("exception"));

            StepVerifier.create(handler.handleTokenNotValid(exchange))
                .expectComplete()
                .verify();

            assertEquals("{\"message\":\"Invalid token\"}", response.getBodyAsString().block());
        }

    }

}
