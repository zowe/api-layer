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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentTypeFilterTest {

    private ContentTypeFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new ContentTypeFilter();
    }

    @Nested
    class WhenRequestHasBody {

        @Test
        void givenJsonContentType_thenRequestProceeds() {
            var request = MockServerHttpRequest.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}");

            var exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(CachedBodyFilter.CACHED_BODY_ATTR, "{}");
            var chain = mock(WebFilterChain.class);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        void givenNoContentType_thenRejectedWithUnsupportedMediaType() {
            var request = MockServerHttpRequest.post("/auth/login")
                .body("{}");

            var exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(CachedBodyFilter.CACHED_BODY_ATTR, "{}");
            var chain = mock(WebFilterChain.class);

            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exchange.getResponse().getStatusCode());
            verifyNoInteractions(chain);
        }

        @Test
        void givenNonJsonContentType_thenRejectedWithUnsupportedMediaType() {
            var request = MockServerHttpRequest.post("/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("username=a&password=b");

            var exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(CachedBodyFilter.CACHED_BODY_ATTR, "username=a&password=b");
            var chain = mock(WebFilterChain.class);

            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exchange.getResponse().getStatusCode());
            verifyNoInteractions(chain);
        }

    }

    @Nested
    class WhenRequestHasNoBody {

        @Test
        void givenNoContentTypeAndNoBody_thenRequestProceeds() {
            var request = MockServerHttpRequest.post("/auth/logout")
                .build();

            var exchange = MockServerWebExchange.from(request);
            var chain = mock(WebFilterChain.class);
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

            verify(chain).filter(exchange);
        }

    }

}
