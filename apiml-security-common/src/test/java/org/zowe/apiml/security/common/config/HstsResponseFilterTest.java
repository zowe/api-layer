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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HstsResponseFilterTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 308, 401, 403, 404, 500})
    void writesHstsWhenSecureResponseCommits(int status) {
        var request = MockServerHttpRequest.get("https://localhost/path").sslInfo(mock(SslInfo.class)).build();
        var exchange = MockServerWebExchange.from(request);
        new HstsResponseFilter(false).filter(exchange, e -> {
            assertNull(e.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
            e.getResponse().setStatusCode(HttpStatusCode.valueOf(status));
            return e.getResponse().setComplete();
        }).block();
        assertEquals("max-age=31536000; includeSubDomains",
            exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @ParameterizedTest
    @CsvSource({"X-Forwarded-Proto,https", "Forwarded,proto=https"})
    void doesNotTrustUnprocessedForwardedHeaders(String header, String value) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/")
            .header(header, value).build());
        new HstsResponseFilter(false).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertNull(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void doesNotWriteHstsOnPlaintextRedirect() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/").build());
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(308));
        new HstsResponseFilter(false).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertNull(exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @Test
    void usesTransportSslInfoEvenWhenTheRequestSchemeIsHttp() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/")
            .sslInfo(mock(SslInfo.class)).build());
        new HstsResponseFilter(false).filter(exchange, e -> e.getResponse().setComplete()).block();
        assertEquals("max-age=31536000; includeSubDomains",
            exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-age=0", "max-age=300", "max-age=31536000; includeSubDomains"})
    void overridesExistingHstsWithoutDuplicateHeaders(String existingPolicy) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/")
            .sslInfo(mock(SslInfo.class)).build());
        new HstsResponseFilter(false).filter(exchange, e -> {
            e.getResponse().getHeaders().set("Strict-Transport-Security", existingPolicy);
            return e.getResponse().setComplete();
        }).block();
        assertEquals(List.of("max-age=31536000; includeSubDomains"),
            exchange.getResponse().getHeaders().get("Strict-Transport-Security"));
    }

    @Test
    void detectsHttpsWithoutSslInfo() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost/").build());

        new HstsResponseFilter(false).filter(exchange, e -> e.getResponse().setComplete()).block();

        assertEquals("max-age=31536000; includeSubDomains", exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @ParameterizedTest
    @CsvSource({"X-Forwarded-Proto,https", "Forwarded,proto=https"})
    void detectsHttpsAfterConfiguredForwardedHeaderProcessing(String header, String value) {
        var request = MockServerHttpRequest.get("http://localhost/").header(header, value).build();
        var forwardedRequest = new ForwardedHeaderTransformer().apply(request);
        assertNull(forwardedRequest.getSslInfo());
        var exchange = MockServerWebExchange.from(request).mutate().request(forwardedRequest).build();

        new HstsResponseFilter(false).filter(exchange, e -> e.getResponse().setComplete()).block();

        assertEquals("max-age=31536000; includeSubDomains", exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 401, 500})
    void writesHstsForAttlsWithoutSslInfo(int status) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost/").build());
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(status));

        new HstsResponseFilter(true).filter(exchange, e -> e.getResponse().setComplete()).block();

        assertEquals("max-age=31536000; includeSubDomains", exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
    }
}
