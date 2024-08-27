/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.zowe.apiml.gateway.controllers.GatewayExceptionHandler;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import reactor.core.publisher.Mono;

import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ForbidEncodedSlashesFilterFactoryTest {

    private static final String ENCODED_REQUEST_URI = "/api/v1/encoded%2fslash";
    private static final String NORMAL_REQUEST_URI = "/api/v1/normal";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @TestPropertySource(properties = "apiml.service.allowEncodedSlashes=true")
    class Responses {

        @Autowired
        ForbidEncodedSlashesFilterFactory gatewayFiler;

        @Test
        void whenUrlDoesNotContainEncodedCharacters() {
            MockServerHttpRequest request = MockServerHttpRequest
                .get(NORMAL_REQUEST_URI)
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            var response = gatewayFiler.apply("").filter(exchange, exchange2 -> {
                exchange.getResponse().setRawStatusCode(200);
                return Mono.empty();
            });
            response.block();
            assertTrue(exchange.getResponse().getStatusCode().is2xxSuccessful());
        }

        @Test
        void whenUrlContainsEncodedCharacters() throws JsonProcessingException {
            MockServerHttpRequest request = MockServerHttpRequest
                .get(ENCODED_REQUEST_URI)
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            var response = gatewayFiler.apply("").filter(exchange, exchange2 -> Mono.empty());
            response.block();
            assertEquals(SC_BAD_REQUEST, exchange.getResponse().getStatusCode().value());
            String body = exchange.getResponse().getBodyAsString().block();
            var message = objectMapper.readValue(body, ApiMessageView.class);
            assertEquals("org.zowe.apiml.gateway.requestContainEncodedSlash", message.getMessages().get(0).getMessageKey());
        }

    }

    @Nested
    class Errors {

        @Test
        void whenJsonProcessorThrowsAnException() throws JsonProcessingException {
            MessageService messageService = mock(MessageService.class);
            doReturn(mock(Message.class)).when(messageService).createMessage(any(), (Object[]) any());
            ObjectMapper objectMapperError = spy(objectMapper);
            GatewayExceptionHandler gatewayExceptionHandler = new GatewayExceptionHandler(objectMapperError, messageService, mock(LocaleContextResolver.class));
            ForbidEncodedSlashesFilterFactory filter = new ForbidEncodedSlashesFilterFactory(gatewayExceptionHandler);

            MockServerHttpRequest request = MockServerHttpRequest
                    .get(ENCODED_REQUEST_URI)
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            doThrow(new JsonGenerationException("error")).when(objectMapperError).writeValueAsBytes(any());

            RuntimeException er = assertThrows(RuntimeException.class, () -> filter.apply("").filter(exchange, e -> Mono.empty()));
            assertEquals("com.fasterxml.jackson.core.JsonGenerationException: error", er.getMessage());
        }

    }

}
