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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ForbidEncodedCharactersFilterFactoryTest {

    private static final String ENCODED_REQUEST_URI = "/api/v1/encoded;ch%25rs";
    private static final String ENCODED_REQUEST_URI_WITH_BACKSLASH = "/api/v1/enc\\oded;ch%25rs";
    private static final String NORMAL_REQUEST_URI = "/api/v1/normal";
    private final ObjectMapper objectMapperError = spy(new ObjectMapper());
    private ForbidEncodedCharactersFilterFactory filter;

    @BeforeEach
    public void setUp() {
        MessageService messageService = new YamlMessageService("/gateway-log-messages.yml");
        filter = new ForbidEncodedCharactersFilterFactory(
            messageService, objectMapperError, mock(LocaleContextResolver.class)
        );
    }

    @Nested
    class Responses {
        @Test
        void givenNormalRequestUri_whenFilterApply_thenSuccess() {
            MockServerHttpRequest request = MockServerHttpRequest
                .get(NORMAL_REQUEST_URI)
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            var response = filter.apply("").filter(exchange, e -> {
                exchange.getResponse().setRawStatusCode(200);
                return Mono.empty();
            });
            response.block();
            assertTrue(exchange.getResponse().getStatusCode().is2xxSuccessful());
        }

        @Test
        void givenRequestUriWithEncodedCharacters_whenFilterApply_thenReturnBadRequest() throws JsonProcessingException, URISyntaxException {
            // A little hack to test request URI with backslashes, otherwise parser in URI.class will throw an exception
            URI uri = new URI(ENCODED_REQUEST_URI); // creating URI without backslashes
            ReflectionTestUtils.setField(uri, "path", ENCODED_REQUEST_URI_WITH_BACKSLASH); // resetting the path with backslashes
            MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, uri)
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            var response = filter.apply("").filter(exchange, e -> Mono.empty());
            response.block();
            assertEquals(SC_BAD_REQUEST, exchange.getResponse().getStatusCode().value());
            String body = exchange.getResponse().getBodyAsString().block();
            var message = objectMapperError.readValue(body, ApiMessageView.class);
            assertEquals("org.zowe.apiml.gateway.requestContainEncodedCharacter", message.getMessages().get(0).getMessageKey());
        }
    }

    @Nested
    class Errors {

        @Test
        void givenRequestUriWithEncodedCharacters_whenJsonProcessorThrowsAnException_thenThrownRuntimeException() throws JsonProcessingException, URISyntaxException {
            MockServerHttpRequest request = MockServerHttpRequest
                .method(HttpMethod.GET, new URI(ENCODED_REQUEST_URI))
                .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            doThrow(new JsonGenerationException("error")).when(objectMapperError).writeValueAsBytes(any());
            RuntimeException er = assertThrows(RuntimeException.class, () -> filter.apply("").filter(exchange, e -> Mono.empty()));
            assertEquals("com.fasterxml.jackson.core.JsonGenerationException: error", er.getMessage());
        }

    }

}
