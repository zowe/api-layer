/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HstsResponseFilterTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 308, 401, 403, 404, 500})
    void writesHstsWhenSecureResponseCommits(int status) {
        var request = MockServerHttpRequest.get("https://localhost/path").sslInfo(mock(SslInfo.class)).build();
        var exchange = MockServerWebExchange.from(request);
        new HstsResponseFilter(31536000, true).filter(exchange, e -> {
            assertNull(e.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
            e.getResponse().setStatusCode(HttpStatusCode.valueOf(status));
            return e.getResponse().setComplete();
        }).block();
        assertEquals("max-age=31536000; includeSubDomains",
            exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void doesNotTrustForwardedHeadersOrSchemeAlone() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/")
            .header("X-Forwarded-Proto", "https").build());
        new HstsResponseFilter(31536000, true).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertNull(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void doesNotWriteHstsOnPlaintextRedirect() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/").build());
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(308));
        new HstsResponseFilter(31536000, true).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertNull(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void supportsAttlsWithoutClientCertificateAfterTransportVerification() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/")
            .sslInfo(mock(SslInfo.class)).build());
        new HstsResponseFilter(31536000, true).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertEquals("max-age=31536000; includeSubDomains",
            exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void existingWriterDoesNotProduceDuplicateHeaders() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/")
            .sslInfo(mock(SslInfo.class)).build());
        new HstsResponseFilter(31536000, true).filter(exchange, e -> {
            new CustomHstsServerHttpHeadersWriter().writeHttpHeaders(e).block();
            return e.getResponse().setComplete();
        }).block();
        assertEquals(1, exchange.getResponse().getHeaders().get("Strict-Transport-Security").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-age=300", "max-age=0", "max-age=600; includeSubDomains; preload"})
    void preservesBackendPolicy(String policy) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/")
            .sslInfo(mock(SslInfo.class)).build());
        new HstsResponseFilter(31536000, true).filter(exchange, e -> {
            e.getResponse().getHeaders().set("Strict-Transport-Security", policy);
            return e.getResponse().setComplete();
        }).block();
        assertEquals(java.util.List.of(policy), exchange.getResponse().getHeaders().get("Strict-Transport-Security"));
    }

    @Test
    void usesConfiguredFallbackPolicy() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/")
            .sslInfo(mock(SslInfo.class)).build());
        new HstsResponseFilter(600, false).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertEquals("max-age=600", exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
        assertThrows(IllegalArgumentException.class, () -> new HstsResponseFilter(-1, false));
    }

    @Test
    void detectsNativeTlsWithoutClientCertificateThroughRequestDecorators() {
        var nativeRequest = new MockHttpServletRequest();
        nativeRequest.setAttribute("jakarta.servlet.request.ssl_session_id", "tls-session");
        var request = mock(AbstractServerHttpRequest.class);
        when(request.getNativeRequest()).thenReturn(nativeRequest);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/").build())
            .mutate().request(new ServerHttpRequestDecorator(request)).build();

        new HstsResponseFilter(600, false).filter(exchange, e -> e.getResponse().setComplete()).block();

        assertEquals("max-age=600", exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void doesNotTrustServletSchemeOrSecureFlagWithoutTlsSession() {
        var nativeRequest = new MockHttpServletRequest();
        nativeRequest.setScheme("https");
        nativeRequest.setSecure(true);
        var request = mock(AbstractServerHttpRequest.class);
        when(request.getNativeRequest()).thenReturn(nativeRequest);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/").build())
            .mutate().request(request).build();

        new HstsResponseFilter(600, false).filter(exchange, e -> e.getResponse().setComplete()).block();

        assertNull(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }
}
