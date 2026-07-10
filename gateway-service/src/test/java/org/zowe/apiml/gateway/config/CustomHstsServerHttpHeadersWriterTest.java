/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomHstsServerHttpHeadersWriterTest {

    @Mock
    private ServerHttpResponse response;
    @Mock
    private ServerWebExchange exchange;

    @Test
    void shouldWriteStrictTransportSecurityHeader() {
        CustomHstsServerHttpHeadersWriter writer = new CustomHstsServerHttpHeadersWriter();
        HttpHeaders headers = new HttpHeaders();
        when(response.getHeaders()).thenReturn(headers);
        when(exchange.getResponse()).thenReturn(response);

        var result = writer.writeHttpHeaders(exchange);

        StepVerifier.create(result)
            .expectComplete()
            .verify();

        assertEquals("max-age=31536000; includeSubDomains", headers.getFirst("Strict-Transport-Security"));
    }

    @Test
    void shouldNotFailWhenHeadersAreAlreadySet() {
        CustomHstsServerHttpHeadersWriter writer = new CustomHstsServerHttpHeadersWriter();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Strict-Transport-Security", "existing-header-value");
        when(response.getHeaders()).thenReturn(headers);
        when(exchange.getResponse()).thenReturn(response);

        var result = writer.writeHttpHeaders(exchange);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        assertEquals("max-age=31536000; includeSubDomains", headers.getFirst("Strict-Transport-Security"));
    }

}
