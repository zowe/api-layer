/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachedBodyFilterTest {

    private CachedBodyFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new CachedBodyFilter();
    }

    @Nested
    class WhenCacheBody {

        @Test
        void givenBody_thenBodyIsAvailableAndRequestReadable() {
            var request = MockServerHttpRequest.post("/auth/login")
                .body("a readable body");

            var exchange = MockServerWebExchange.from(request);
            var chain = mock(WebFilterChain.class);

            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

            assertEquals("a readable body", exchange.getAttribute(CachedBodyFilter.CACHED_BODY_ATTR));

            StepVerifier.create(DataBufferUtils.join(exchange.getRequest().getBody()))
                .expectNextMatches(this::assertDataBuffer)
                .verifyComplete();
        }

        private boolean assertDataBuffer(DataBuffer dataBuffer) {
            var bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return new String(bytes).equals("a readable body");
        }

        @Test
        void givenEmptyBody_thenClean() {
            var requestWithoutBody = MockServerHttpRequest.post("/auth/login")
                .build();

            var exchange = MockServerWebExchange.from(requestWithoutBody);
            var chain = mock(WebFilterChain.class);

            when(chain.filter(exchange)).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

            assertNull(exchange.getAttribute(CachedBodyFilter.CACHED_BODY_ATTR));
        }

    }

}
