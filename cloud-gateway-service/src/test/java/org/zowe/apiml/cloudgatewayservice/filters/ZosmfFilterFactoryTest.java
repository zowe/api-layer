/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.zaas.zosmf.ZosmfResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZosmfFilterFactoryTest {

    private static MockWebServer mockWebServer;
    private static ObjectMapper objectMapper;

    SslInfo sslInfo = mock(SslInfo.class);
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);

    HttpHeaders headers = mock(HttpHeaders.class);

    ZosmfFilterFactory factory;
    ZosmfFilterFactory.Config config;


    InstanceInfoService instanceInfoService = mock(InstanceInfoService.class);
    ServiceInstance serviceInstance = mock(ServiceInstance.class);
    MessageService messageService = YamlMessageServiceInstance.getInstance();

    {
        messageService.loadMessages("/cloud-gateway-log-messages.yml");
    }

    @BeforeAll
    static void init() throws IOException {
        objectMapper = new ObjectMapper();
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setup() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        factory = new ZosmfFilterFactory(WebClient.create(), instanceInfoService, messageService);
        config = new ZosmfFilterFactory.Config();

        when(instanceInfoService.getServiceInstance("gateway")).thenReturn(Mono.just(Collections.singletonList(serviceInstance)));
        when(serviceInstance.getScheme()).thenReturn("http");
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(mockWebServer.getPort());
        when(serviceInstance.getServiceId()).thenReturn("gateway");

        ServerHttpRequest.Builder builder = new ServerHttpRequestBuilderMock();
        ServerWebExchange.Builder exchangeBuilder = new ServerWebExchangeBuilderMock();

        when(exchange.getRequest()).thenReturn(request);

        when(request.getHeaders()).thenReturn(headers);
        when(request.mutate()).thenReturn(builder);
        when(exchange.mutate()).thenReturn(exchangeBuilder);

        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void givenZosmfTokenFromZaas_thenPopulateHeaders() throws Exception {
        ZosmfResponse zosmfResponse = new ZosmfResponse("cookie-name", "test-token");
        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(zosmfResponse))
            .addHeader("Content-Type", "application/json"));


        GatewayFilter filter = factory.apply(config);
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertNotNull(exchange.getRequest().getHeaders().get("Cookie"));
        assertEquals(1, exchange.getRequest().getHeaders().get("Cookie").size());
        assertEquals("cookie-name=test-token", exchange.getRequest().getHeaders().get("Cookie").get(0));
    }

    public class ServerHttpRequestBuilderMock implements ServerHttpRequest.Builder {
        HttpHeaders headers = new HttpHeaders();

        @Override
        public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
            return null;
        }

        @Override
        public ServerHttpRequest.Builder uri(URI uri) {
            return null;
        }

        @Override
        public ServerHttpRequest.Builder path(String path) {
            return null;
        }

        @Override
        public ServerHttpRequest.Builder contextPath(String contextPath) {
            return null;
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
            return null;
        }

        @Override
        public ServerHttpRequest.Builder remoteAddress(InetSocketAddress remoteAddress) {
            return null;
        }

        @Override
        public ServerHttpRequest build() {
            when(request.getHeaders()).thenReturn(headers);
            return request;
        }
    }

    public class ServerWebExchangeBuilderMock implements ServerWebExchange.Builder {
        ServerHttpRequest request;

        @Override
        public ServerWebExchange.Builder request(Consumer<ServerHttpRequest.Builder> requestBuilderConsumer) {
            return null;
        }

        @Override
        public ServerWebExchange.Builder request(ServerHttpRequest request) {
            this.request = request;
            return this;
        }

        @Override
        public ServerWebExchange.Builder response(ServerHttpResponse response) {
            return null;
        }

        @Override
        public ServerWebExchange.Builder principal(Mono<Principal> principalMono) {
            return null;
        }

        @Override
        public ServerWebExchange build() {
            when(exchange.getRequest()).thenReturn(request);
            return exchange;
        }
    }

}
