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

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHeadersGlobalFilterTest {

    private final SecurityHeadersGlobalFilter filter = new SecurityHeadersGlobalFilter();

    @Test
    void shouldAddSecurityHeadersToResponse() {
        // 1. Create a mock request and exchange instance
        MockServerHttpRequest request = MockServerHttpRequest.get("/apicatalog/ui/v1/index.html").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // 2. Mock the GatewayFilterChain to simulate a successful downstream filter execution
        GatewayFilterChain filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // 3. Execute the filter and use StepVerifier to handle the reactive lifecycle
        Mono<Void> result = filter.filter(exchange, filterChain);

        StepVerifier.create(result)
            .verifyComplete(); // Ensures the Mono chain finishes cleanly

        // 4. Assert that the headers were successfully injected into the response
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();

        assertEquals("DENY", responseHeaders.getFirst("X-Frame-Options"));
        assertEquals("nosniff", responseHeaders.getFirst("X-Content-Type-Options"));
    }
}
