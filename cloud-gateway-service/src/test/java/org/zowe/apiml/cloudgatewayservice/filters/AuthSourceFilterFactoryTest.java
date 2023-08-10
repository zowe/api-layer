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
import org.zowe.apiml.cloudgatewayservice.security.AuthSourceSign;
import org.zowe.apiml.constants.ApimlConstants;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthSourceFilterFactoryTest {

    private static final String AUTH_SOURCE_HEADER = "auth-source";
    private static final String AUTH_SIGNATURE_HEADER = "auth-signature";
    private static final byte[] CERTIFICATE_BYTES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    private static final byte[] CERTIFICATE_SIGNATURE = "1234567890".getBytes();
    private final X509Certificate[] x509Certificates = new X509Certificate[1];

    SslInfo sslInfo = mock(SslInfo.class);
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest request = mock(ServerHttpRequest.class);
    X509Certificate certificate = mock(X509Certificate.class);
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    AuthSourceSign authSourceSign = mock(AuthSourceSign.class);
    AuthSourceFilterFactory factory;
    AuthSourceFilterFactory.Config config;

    @BeforeEach
    void setup() throws CertificateEncodingException, SignatureException {
        factory = new AuthSourceFilterFactory(authSourceSign);
        config = new AuthSourceFilterFactory.Config();
        config.setAuthSourceHeader(AUTH_SOURCE_HEADER);
        config.setAuthSignatureHeader(AUTH_SIGNATURE_HEADER);

        ServerHttpRequest.Builder builder = new ServerHttpRequestBuilderMock();
        ServerWebExchange.Builder exchangeBuilder = new ServerWebExchangeBuilderMock();
        x509Certificates[0] = certificate;
        when(certificate.getEncoded()).thenReturn(CERTIFICATE_BYTES);
        when(authSourceSign.sign(CERTIFICATE_BYTES)).thenReturn(CERTIFICATE_SIGNATURE);

        when(exchange.getRequest()).thenReturn(request);

        when(request.getSslInfo()).thenReturn(sslInfo);
        when(request.mutate()).thenReturn(builder);

        when(sslInfo.getPeerCertificates()).thenReturn(x509Certificates);

        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
    }
    @Test
    void givenCertificateInRequest_thenAddHeaders() throws Exception {
        GatewayFilter filter = factory.apply(config);
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
        assertNotNull(exchange.getRequest().getHeaders().get(AUTH_SOURCE_HEADER));
        assertNotNull(exchange.getRequest().getHeaders().get(AUTH_SIGNATURE_HEADER));
        assertEquals(Base64.getEncoder().encodeToString(CERTIFICATE_BYTES), exchange.getRequest().getHeaders().get(AUTH_SOURCE_HEADER).get(0));
        assertEquals(Base64.getEncoder().encodeToString(CERTIFICATE_SIGNATURE), exchange.getRequest().getHeaders().get(AUTH_SIGNATURE_HEADER).get(0));
    }

    @Test
    void givenCertificateWithIncorrectEncoding_thenProvideInfoInHeader() throws Exception {
        GatewayFilter filter = factory.apply(config);
        when(certificate.getEncoded()).thenThrow(new CertificateEncodingException("incorrect encoding"));
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertNotNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
        assertEquals("Invalid client certificate in request. Error message: incorrect encoding", exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER).get(0));
    }

    @Test
    void givenErrorDuringSigning_thenProvideErrorInHeader() throws Exception {
        GatewayFilter filter = factory.apply(config);
        when(authSourceSign.sign(CERTIFICATE_BYTES)).thenThrow(new SignatureException("invalid private key"));
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertNotNull(exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER));
        assertEquals("Failed to sign the client certificate in request. Error message: invalid private key", exchange.getRequest().getHeaders().get(ApimlConstants.AUTH_FAIL_HEADER).get(0));
    }

    @Test
    void givenNoCertificateInRequest_thenContinueFilterChainWithUntouchedHeaders() {
        GatewayFilter filter = factory.apply(config);
        when(sslInfo.getPeerCertificates()).thenReturn(null);
        Mono<Void> result = filter.filter(exchange, chain);
        result.block();
        assertNull(exchange.getRequest().getHeaders());
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
