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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecFetchSiteFilterTest {

    private static final String ALLOWED_ORIGIN = "https://trusted.example.com";
    private static final String OTHER_ORIGIN = "https://evil.example.com";

    private CorsConfigurationSource corsConfigurationSource;
    private AtomicBoolean chainCalled;
    private WebFilterChain chain;

    @BeforeEach
    void setUp() {
        corsConfigurationSource = mock(CorsConfigurationSource.class);
        chainCalled = new AtomicBoolean(false);
        chain = exchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };
    }

    private SecFetchSiteFilter filter(boolean enabled) {
        return new SecFetchSiteFilter(corsConfigurationSource, enabled);
    }

    private MockServerWebExchange exchange(HttpMethod method, String secFetchSite, String origin) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, "/service/api/v1/foo");
        if (secFetchSite != null) {
            builder.header(SecFetchSiteFilter.SEC_FETCH_SITE_HEADER, secFetchSite);
        }
        if (origin != null) {
            builder.header(HttpHeaders.ORIGIN, origin);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private void mockCorsAllowing(String... origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        for (String origin : origins) {
            configuration.addAllowedOrigin(origin);
        }
        when(corsConfigurationSource.getCorsConfiguration(any())).thenReturn(configuration);
    }

    private void assertAllowed(MockServerWebExchange exchange, SecFetchSiteFilter filter) {
        filter.filter(exchange, chain).block();
        assertTrue(chainCalled.get(), "expected the filter chain to continue");
        assertNull(exchange.getResponse().getStatusCode(), "expected no status to be set by the filter");
    }

    private void assertRejected(MockServerWebExchange exchange, SecFetchSiteFilter filter) {
        filter.filter(exchange, chain).block();
        assertFalse(chainCalled.get(), "expected the filter chain to be short-circuited");
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Nested
    class WhenEnabled {

        @Test
        void givenNoSecFetchSiteHeader_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, null, null), filter(true));
        }

        @Test
        void givenSameOrigin_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, "same-origin", null), filter(true));
        }

        @Test
        void givenNone_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, "none", null), filter(true));
        }

        @Test
        void givenSafeMethodCrossSite_thenAllowed() {
            assertAllowed(exchange(HttpMethod.GET, "cross-site", OTHER_ORIGIN), filter(true));
        }

        @Test
        void givenPreflightOptionsCrossSite_thenAllowed() {
            assertAllowed(exchange(HttpMethod.OPTIONS, "cross-site", OTHER_ORIGIN), filter(true));
        }

        @Test
        void givenCrossSiteFromCorsAllowedOrigin_thenAllowed() {
            mockCorsAllowing(ALLOWED_ORIGIN);
            assertAllowed(exchange(HttpMethod.POST, "cross-site", ALLOWED_ORIGIN), filter(true));
        }

        @Test
        void givenCrossSiteFromNonAllowedOrigin_thenRejected() {
            mockCorsAllowing(ALLOWED_ORIGIN);
            assertRejected(exchange(HttpMethod.POST, "cross-site", OTHER_ORIGIN), filter(true));
        }

        @Test
        void givenSameSiteFromCorsAllowedOrigin_thenAllowed() {
            mockCorsAllowing(ALLOWED_ORIGIN);
            assertAllowed(exchange(HttpMethod.POST, "same-site", ALLOWED_ORIGIN), filter(true));
        }

        @Test
        void givenSameSiteFromNonAllowedOrigin_thenRejected() {
            mockCorsAllowing(ALLOWED_ORIGIN);
            assertRejected(exchange(HttpMethod.POST, "same-site", OTHER_ORIGIN), filter(true));
        }

        @Test
        void givenCrossSiteWithoutOriginHeader_thenRejected() {
            mockCorsAllowing(ALLOWED_ORIGIN);
            assertRejected(exchange(HttpMethod.POST, "cross-site", null), filter(true));
        }

        @Test
        void givenCrossSiteAndNoCorsConfigForPath_thenRejected() {
            when(corsConfigurationSource.getCorsConfiguration(any())).thenReturn(null);
            assertRejected(exchange(HttpMethod.POST, "cross-site", OTHER_ORIGIN), filter(true));
        }
    }

    @Nested
    class WhenDisabled {

        @Test
        void givenCrossSiteFromNonAllowedOrigin_thenAllowed() {
            assertAllowed(exchange(HttpMethod.POST, "cross-site", OTHER_ORIGIN), filter(false));
        }
    }

}
