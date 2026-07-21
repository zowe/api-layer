/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecFetchSiteFilterTest {

    private static final String ORIGIN = "https://evil.example.com";

    private MockServerWebExchange exchange(HttpMethod method, String site, String mode, String dest) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, "/service/api/v1/foo")
            .header(HttpHeaders.ORIGIN, ORIGIN);
        if (site != null) {
            builder.header(SecFetchSiteFilter.SEC_FETCH_SITE_HEADER, site);
        }
        if (mode != null) {
            builder.header(SecFetchSiteFilter.SEC_FETCH_MODE_HEADER, mode);
        }
        if (dest != null) {
            builder.header(SecFetchSiteFilter.SEC_FETCH_DEST_HEADER, dest);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private void assertAllowed(MockServerWebExchange exchange, SecFetchSiteFilter filter) {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "expected the filter chain to continue");
        assertNull(exchange.getResponse().getStatusCode(), "expected no status to be set by the filter");
    }

    private void assertRejected(MockServerWebExchange exchange, SecFetchSiteFilter filter) {
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };
        filter.filter(exchange, chain).block();
        assertFalse(chainCalled.get(), "expected the filter chain to be short-circuited");
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertEquals("Invalid CORS request", exchange.getResponse().getBodyAsString().block());
    }

    @Nested
    class WhenCorsEnabled {

        private final SecFetchSiteFilter filter = new SecFetchSiteFilter(true);

        @ParameterizedTest
        @ValueSource(strings = {"same-origin", "same-site", "none", "cross-site"})
        void givenAnySite_thenAllowedAndDeferredToCors(String site) {
            assertAllowed(exchange(HttpMethod.POST, site, null, null), filter);
        }

        @Test
        void givenNoSecFetchSiteHeader_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, null, null, null), filter);
        }
    }

    @Nested
    class WhenCorsDisabled {

        private final SecFetchSiteFilter filter = new SecFetchSiteFilter(false);

        @Test
        void givenNoSecFetchSiteHeader_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, null, null, null), filter);
        }

        @ParameterizedTest
        @ValueSource(strings = {"same-origin", "same-site", "none"})
        void givenSafeSite_thenAllowed(String site) {
            assertAllowed(exchange(HttpMethod.POST, site, null, null), filter);
        }

        @Test
        void givenCrossSiteStateChanging_thenRejected() {
            assertRejected(exchange(HttpMethod.POST, "cross-site", null, null), filter);
        }

        @Test
        void givenCrossSiteNonNavigationGet_thenRejected() {
            assertRejected(exchange(HttpMethod.GET, "cross-site", "cors", null), filter);
        }

        @Test
        void givenCrossSiteSafeTopLevelNavigation_thenAllowed() {
            assertAllowed(exchange(HttpMethod.GET, "cross-site", "navigate", "document"), filter);
        }

        @Test
        void givenCrossSiteNavigationIntoObject_thenRejected() {
            assertRejected(exchange(HttpMethod.GET, "cross-site", "navigate", "object"), filter);
        }

        @Test
        void givenCrossSiteNavigationWithUnsafeMethod_thenRejected() {
            assertRejected(exchange(HttpMethod.POST, "cross-site", "navigate", "document"), filter);
        }

        @Test
        void givenSecFetchSiteValueIsCaseInsensitive_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, "Same-Origin", null, null), filter);
        }
    }

}
