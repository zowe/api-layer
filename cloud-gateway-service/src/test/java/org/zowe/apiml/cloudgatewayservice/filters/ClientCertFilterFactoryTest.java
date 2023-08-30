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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientCertFilterFactoryTest {

    private static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final byte[] CERTIFICATE_BYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    private static final String ENCODED_CERT = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo="; //Base64.getEncoder().encodeToString(CERTIFICATE_BYTES);
    private final X509Certificate[] x509Certificates = new X509Certificate[1];

    SslInfo sslInfo = mock(SslInfo.class);
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    ClientCertFilterFactory filterFactory;
    ClientCertFilterFactory.Config filterConfig = new ClientCertFilterFactory.Config();
    ServerHttpRequest.Builder requestBuilder;

    @BeforeEach
    void setup() {
        x509Certificates[0] = mock(X509Certificate.class);
        filterFactory = new ClientCertFilterFactory();
        requestBuilder = new ServerHttpRequestBuilderMock();
        ServerWebExchange.Builder exchangeBuilder = new ServerWebExchangeBuilderMock();

        when(sslInfo.getPeerCertificates()).thenReturn(x509Certificates);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getSslInfo()).thenReturn(sslInfo);

        when(request.mutate()).thenReturn(requestBuilder);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Nested
    class GivenValidCertificateInRequest {

        @BeforeEach
        void setup() throws CertificateException {
            when(x509Certificates[0].getEncoded()).thenReturn(CERTIFICATE_BYTES);
        }

        @Test
        void whenEnabled_thenAddHeaderToRequest() {
            filterConfig.setForwardingEnabled("true");
            GatewayFilter filter = filterFactory.apply(filterConfig);
            Mono<Void> result = filter.filter(exchange, chain);
            result.block();

            assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
            assertNotNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
            assertEquals(ENCODED_CERT, exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER).get(0));
        }

        @Test
        void whenDisabled_thenNoHeadersInRequest() {
            filterConfig.setForwardingEnabled("false");
            GatewayFilter filter = filterFactory.apply(filterConfig);
            Mono<Void> result = filter.filter(exchange, chain);
            result.block();

            assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
            assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
        }

        @Nested
        class WhenClientCertHeaderIsAlreadyInRequest {

            @BeforeEach
            void setup() {
                requestBuilder.header(CLIENT_CERT_HEADER, "This value cannot pass through the filter.");
            }

            @Test
            void whenEnabled_thenHeaderContainsNewValue() {
                filterConfig.setForwardingEnabled("true");
                GatewayFilter filter = filterFactory.apply(filterConfig);
                Mono<Void> result = filter.filter(exchange, chain);
                result.block();

                assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
                assertNotNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
                assertEquals(ENCODED_CERT, exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER).get(0));
            }

            @Test
            void whenDisabled_thenNoHeadersInRequest() {
                filterConfig.setForwardingEnabled("false");
                GatewayFilter filter = filterFactory.apply(filterConfig);
                Mono<Void> result = filter.filter(exchange, chain);
                result.block();

                assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
                assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
            }

            @Nested
            class WhenNoSSLSessionInformation {

                @BeforeEach
                void setup() {
                    when(request.getSslInfo()).thenReturn(null);
                }

                @ParameterizedTest
                @ValueSource(strings = {"true", "false"})
                void thenNoHeadersInRequest(String enabled) {
                    filterConfig.setForwardingEnabled(enabled);
                    GatewayFilter filter = filterFactory.apply(filterConfig);
                    Mono<Void> result = filter.filter(exchange, chain);
                    result.block();

                    assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
                    assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
                }
            }
        }

        @Nested
        class WhenNoSSLSessionInformation {

            @BeforeEach
            void setup() {
                when(request.getSslInfo()).thenReturn(null);
            }

            @ParameterizedTest
            @ValueSource(strings = {"true", "false"})
            void thenNoHeadersInRequest(String enabled) {
                filterConfig.setForwardingEnabled(enabled);
                GatewayFilter filter = filterFactory.apply(filterConfig);
                Mono<Void> result = filter.filter(exchange, chain);
                result.block();

                assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
                assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
            }
        }
    }


    @Nested
    class GivenInvalidCertificateInRequest {

        @BeforeEach
        void setup() throws CertificateEncodingException {
            filterConfig.setForwardingEnabled("true");
            requestBuilder.header(CLIENT_CERT_HEADER, "This value cannot pass through the filter.");
            when(x509Certificates[0].getEncoded()).thenThrow(new CertificateEncodingException("incorrect encoding"));
        }


        @Test
        void thenProvideInfoInFailHeader() {
            GatewayFilter filter = filterFactory.apply(filterConfig);
            Mono<Void> result = filter.filter(exchange, chain);
            result.block();

            assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
            assertNotNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
            assertEquals("Invalid client certificate in request. Error message: incorrect encoding", exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER).get(0));
        }
    }

    @Nested
    class GivenNoClientCertificateInRequest {

        @BeforeEach
        void setup() {
            requestBuilder.header(CLIENT_CERT_HEADER, "This value cannot pass through the filter.");
            when(sslInfo.getPeerCertificates()).thenReturn(new X509Certificate[0]);
        }

        @ParameterizedTest
        @ValueSource(strings = {"true", "false"})
        void thenContinueFilterChainWithoutClientCertHeader(String enabled) {
            filterConfig.setForwardingEnabled(enabled);
            GatewayFilter filter = filterFactory.apply(filterConfig);
            Mono<Void> result = filter.filter(exchange, chain);
            result.block();
            assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
            assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
        }
    }

    @Nested
    class GivenNoSSLSessionInformationInRequest {

        @BeforeEach
        void setup() {
            requestBuilder.header(CLIENT_CERT_HEADER, "This value cannot pass through the filter.");
            when(request.getSslInfo()).thenReturn(null);
        }

        @ParameterizedTest
        @ValueSource(strings = {"true", "false"})
        void thenContinueFilterChainWithoutClientCertHeader(String enabled) {
            filterConfig.setForwardingEnabled(enabled);
            GatewayFilter filter = filterFactory.apply(filterConfig);
            Mono<Void> result = filter.filter(exchange, chain);
            result.block();

            assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
            assertNull(exchange.getRequest().getHeaders().get(CLIENT_CERT_HEADER));
        }
    }

    private class ServerHttpRequestBuilderMock implements ServerHttpRequest.Builder {
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

    private class ServerWebExchangeBuilderMock implements ServerWebExchange.Builder {
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
