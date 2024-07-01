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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TomcatFilterTest {

    private static final String ALLOW_ENCODED_SLASHES_FIELD = "allowEncodedSlashes";
    private static final String ENCODED_REQUEST_URI = "/api/v1/encoded%2fslash";
    private static final String NORMAL_REQUEST_URI = "/api/v1/normal";

    @Mock
    private ObjectMapper objectMapper;

    private SslInfo sslInfo;
    private ServerWebExchange exchange;
    private ServerHttpRequest request;
    private ServerHttpResponse response;
    private WebFilterChain chain;
    private MessageService messageService;
    private TomcatFilter filter;

    @BeforeAll
    static void initMessageService() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/gateway-log-messages.yml");
    }

    @BeforeEach
    void setup() {
        sslInfo = mock(SslInfo.class);
        exchange = mock(ServerWebExchange.class);
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        chain = mock(WebFilterChain.class);
        messageService = YamlMessageServiceInstance.getInstance();

        ServerHttpRequest.Builder requestBuilder = new ServerHttpRequestBuilderMock();
        ServerWebExchange.Builder exchangeBuilder = new ServerWebExchangeBuilderMock();

        when(exchange.getRequest()).thenReturn(request);
        when(request.getSslInfo()).thenReturn(sslInfo);
        when(request.mutate()).thenReturn(requestBuilder);
        when(exchange.mutate()).thenReturn(exchangeBuilder);

        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void shouldRejectEncodedSlashRequestsWhenConfiguredToReject() throws IOException, ServletException {
        filter = new TomcatFilter(messageService, objectMapper);

        when(request.getURI()).thenReturn(URI.create(ENCODED_REQUEST_URI));
        when(response.setStatusCode(HttpStatus.BAD_REQUEST)).thenReturn(true);

        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());

    }

//    @Test
//    void shouldAllowNonEncodedSlashRequestsAndMoveToNextFilterWhenConfiguredToReject() throws IOException, ServletException {
//        filter = new TomcatFilter(messageService, objectMapper);
////        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, false);
//
//        when(request.getURI()).thenReturn(URI.create(NORMAL_REQUEST_URI));
//        filter.filter(exchange, chain).block();
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(chain).filter(exchange);
//    }
//
//    @Test
//    void shouldAllowAnyRequestAndMoveToNextFilterWhenConfiguredToAllow() throws IOException, ServletException {
//        filter = new TomcatFilter(messageService, objectMapper);
////        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, true);
//
//        when(request.getURI()).thenReturn(URI.create(NORMAL_REQUEST_URI));
//        filter.filter(exchange, chain).block();
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(chain).filter(exchange);
//
//        when(request.getURI()).thenReturn(URI.create(ENCODED_REQUEST_URI));
//        filter.filter(exchange, chain).block();
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(chain).filter(exchange);
//    }
//
//    @Test
//    void shouldThrowServletExceptionOnIOExceptionWhenWritingResponse() throws IOException {
//        filter = new TomcatFilter(messageService, objectMapper);
////        ReflectionTestUtils.setField(filter, ALLOW_ENCODED_SLASHES_FIELD, false);
//
//        when(request.getURI()).thenReturn(URI.create(ENCODED_REQUEST_URI));
////        when(response.getWriter()).thenThrow(new IOException());
//
//        assertThrows(ServletException.class, () -> filter.filter(exchange, chain).block());
//    }

    public class ServerHttpRequestBuilderMock implements ServerHttpRequest.Builder {
        private HttpHeaders headers = new HttpHeaders();

        @Override
        public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
            return this;
        }

        @Override
        public ServerHttpRequest.Builder uri(URI uri) {
            return this;
        }

        @Override
        public ServerHttpRequest.Builder path(String path) {
            return this;
        }

        @Override
        public ServerHttpRequest.Builder contextPath(String contextPath) {
            return this;
        }

        @Override
        public ServerHttpRequest.Builder header(String headerName, String... headerValues) {
            headers.add(headerName, headerValues[0]);
            return this;
        }

        @Override
        public ServerHttpRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
            headersConsumer.accept(this.headers);
            return this;
        }

        @Override
        public ServerHttpRequest.Builder sslInfo(SslInfo sslInfo) {
            return this;
        }

        @Override
        public ServerHttpRequest.Builder remoteAddress(InetSocketAddress remoteAddress) {
            return this;
        }

        @Override
        public ServerHttpRequest build() {
            when(request.getHeaders()).thenReturn(headers);
            return request;
        }
    }

    public class ServerWebExchangeBuilderMock implements ServerWebExchange.Builder {
        private ServerHttpRequest request;

        @Override
        public ServerWebExchange.Builder request(Consumer<ServerHttpRequest.Builder> requestBuilderConsumer) {
            return this;
        }

        @Override
        public ServerWebExchange.Builder request(ServerHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public ServerWebExchange.Builder response(ServerHttpResponse response) {
            return this;
        }

        @Override
        public ServerWebExchange.Builder principal(Mono<Principal> principalMono) {
            return this;
        }

        @Override
        public ServerWebExchange build() {
            when(exchange.getRequest()).thenReturn(request);
            return exchange;
        }
    }
}
