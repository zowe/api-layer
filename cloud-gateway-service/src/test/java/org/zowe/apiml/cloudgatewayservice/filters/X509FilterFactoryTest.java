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
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import reactor.core.publisher.Mono;

import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.cloudgatewayservice.config.Constants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

class X509FilterFactoryTest {
    public static final String ALL_HEADERS = "X-Certificate-Public,X-Certificate-DistinguishedName,X-Certificate-CommonName";
    private final X509Certificate[] x509Certificates = new X509Certificate[1];

    SslInfo sslInfo = mock(SslInfo.class);
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    X509Certificate certificate = mock(X509Certificate.class);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    X509FilterFactory factory;
    X509FilterFactory.Config config;
    MessageService messageService = YamlMessageServiceInstance.getInstance();

    {
        messageService.loadMessages("/cloud-gateway-log-messages.yml");
    }

    @BeforeEach
    void setup() {
        factory = new X509FilterFactory(messageService);
        config = new X509FilterFactory.Config();
        config.setHeaders(ALL_HEADERS);

        ServerHttpRequest.Builder builder = new ServerHttpRequestBuilderMock();
        ServerWebExchange.Builder exchangeBuilder = new ServerWebExchangeBuilderMock();
        x509Certificates[0] = certificate;

        when(exchange.getRequest()).thenReturn(request);

        when(request.getSslInfo()).thenReturn(sslInfo);
        when(request.mutate()).thenReturn(builder);

        when(sslInfo.getPeerCertificates()).thenReturn(x509Certificates);

        when(certificate.getSubjectDN()).thenReturn(new X500Principal("CN=user, OU=JavaSoft, O=Sun Microsystems, C=US"));
        when(exchange.mutate()).thenReturn(exchangeBuilder);

        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getAttributes()).thenReturn(attributes);

        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }

    @Test
    void givenCertificateInRequest_thenPopulateHeaders() throws Exception {
        GatewayFilter filter = factory.apply(config);
        when(certificate.getEncoded()).thenReturn(new byte[2]);
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertEquals("user", exchange.getRequest().getHeaders().get("X-Certificate-CommonName").get(0));
        assertEquals(Boolean.TRUE, exchange.getAttributes().get(HTTP_CLIENT_USE_CLIENT_CERTIFICATE));
    }

    @Test
    void givenCertificateWithIncorrectEncoding_thenProvideInfoInHeader() throws Exception {

        GatewayFilter filter = factory.apply(config);
        when(certificate.getEncoded()).thenThrow(new CertificateEncodingException("incorrect encoding"));
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertEquals("Invalid client certificate in request. Error message: incorrect encoding", exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER).get(0));
        assertNull(exchange.getAttribute(HTTP_CLIENT_USE_CLIENT_CERTIFICATE));
    }

    @Test
    void givenNoCertificateInRequest_thenContinueFilterChainWithUntouchedHeaders() {

        GatewayFilter filter = factory.apply(config);

        when(sslInfo.getPeerCertificates()).thenReturn(null);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(exchange.getResponse()).thenReturn(response);
        HttpHeaders responseHeaders = new HttpHeaders();
        when(response.getHeaders()).thenReturn(responseHeaders);
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertEquals("ZWEAG167E No client certificate provided in the request", exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER).get(0));
        assertEquals("ZWEAG167E No client certificate provided in the request", responseHeaders.get(ApimlConstants.AUTH_FAIL_HEADER).get(0));
        assertNull(exchange.getAttribute(HTTP_CLIENT_USE_CLIENT_CERTIFICATE));
    }

    @Test
    void givenCorrectDN_returnCommonName() throws Exception {
        assertEquals("user", X509FilterFactory.getCommonName(new LdapName("CN=user, OU=JavaSoft, O=Sun Microsystems, C=US")));
    }

    @Test
    void givenDNWithoutCommonName_returnNull() throws Exception {
        assertNull(X509FilterFactory.getCommonName(new LdapName("OU=JavaSoft, O=Sun Microsystems, C=US")));
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
