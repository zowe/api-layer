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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecFetchSiteFilterTest {

    private static final String ORIGIN = "https://evil.example.com";
    private static final String PATH = "/service/api/v1/foo";
    private static final String UNPROTECTED_PATH = "/other-service/api/v1/foo";

    /**
     * CORS is configured for {@link #PATH} only, mirroring the Gateway registering a configuration per
     * service ({@code ServiceCorsUpdater}) rather than for every path.
     */
    private static CorsConfigurationSource corsConfiguredForServicePath() {
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/service/**", new CorsConfiguration());
        return source;
    }

    private SecFetchSiteFilter filter(boolean corsEnabled) {
        return new SecFetchSiteFilter(corsEnabled, corsConfiguredForServicePath(), Set.of("navigate", "same-origin"), null, null);
    }

    private MockServerWebExchange exchange(HttpMethod method, String site, String mode, String dest) {
        return exchange(method, PATH, ORIGIN, site, mode, dest);
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String origin, String site, String mode, String dest) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.method(method, path);
        if (origin != null) {
            builder.header(HttpHeaders.ORIGIN, origin);
        }
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
        assertEquals("Access denied.", exchange.getResponse().getBodyAsString().block());
    }

    @Nested
    class GivenSafeSecFetchSiteHeader {

        @ParameterizedTest
        @ValueSource(strings = {"same-origin", "none", "Same-Origin", "NONE"})
        @NullSource
        void thenAllowed(String site) {
            assertAllowed(exchange(HttpMethod.POST, site, null, null), filter(false));
        }
    }

    @Nested
    class WhenCorsEnabled {

        private SecFetchSiteFilter filter;

        @BeforeEach
        void setUp() {
            filter = filter(true);
        }

        @ParameterizedTest
        @ValueSource(strings = {"cross-site", "same-site", "Cross-Site", "whatever"})
        void givenOriginAndCorsConfiguredForPath_thenDeferredToCors(String site) {
            assertAllowed(exchange(HttpMethod.POST, site, null, null), filter);
        }

        @ParameterizedTest
        @ValueSource(strings = {"cross-site", "same-site"})
        void givenNoCorsConfigurationForPath_thenRejected(String site) {
            assertRejected(exchange(HttpMethod.POST, UNPROTECTED_PATH, ORIGIN, site, null, null), filter);
        }

        @ParameterizedTest
        @ValueSource(strings = {"cross-site", "same-site"})
        void givenNoOrigin_thenRejected(String site) {
            // Without an Origin the request is not a CORS request, so the CORS filter never judges it
            assertRejected(exchange(HttpMethod.POST, PATH, null, site, null, null), filter);
        }

        @Test
        void givenNoCorsConfigurationSource_thenRejected() {
            var noSource = new SecFetchSiteFilter(true, null, Set.of("navigate"), null, null);
            assertRejected(exchange(HttpMethod.POST, "cross-site", null, null), noSource);
        }
    }

    @Nested
    class WhenCorsDisabled {

        private SecFetchSiteFilter filter;

        @BeforeEach
        void setUp() {
            filter = filter(false);
        }

        @ParameterizedTest
        @ValueSource(strings = {"cross-site", "same-site"})
        void givenOriginAndCorsConfiguredForPath_thenNotDeferredToCors(String site) {
            assertRejected(exchange(HttpMethod.POST, site, null, null), filter);
        }

        @Nested
        class GivenCrossSiteRequest {

            @Nested
            class WhenNavigateMode {

                @ParameterizedTest
                @CsvSource({"GET,navigate", "HEAD,navigate", "GET,same-origin", "GET,NAVIGATE"})
                void givenSafeMethod_thenAllowed(String method, String mode) {
                    assertAllowed(exchange(HttpMethod.valueOf(method), "cross-site", mode, "document"), filter);
                }

                @ParameterizedTest
                @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
                void givenUnsafeMethod_thenRejected(String method) {
                    assertRejected(exchange(HttpMethod.valueOf(method), "cross-site", "navigate", "document"), filter);
                }

                @ParameterizedTest
                @ValueSource(strings = {"POST", "PUT"})
                void givenUnsafeMethodOnAllowListedPath_thenAllowed(String method) {
                    var allowingPath = new SecFetchSiteFilter(false, null, Set.of("navigate"), null, List.of("/service/**"));
                    assertAllowed(exchange(HttpMethod.valueOf(method), "cross-site", "navigate", "document"), allowingPath);
                }

                @ParameterizedTest
                @ValueSource(strings = {"POST", "PUT"})
                void givenUnsafeMethodOnOtherPath_thenRejected(String method) {
                    var allowingPath = new SecFetchSiteFilter(false, null, Set.of("navigate"), null, List.of("/service/**"));
                    assertRejected(exchange(HttpMethod.valueOf(method), UNPROTECTED_PATH, ORIGIN, "cross-site", "navigate", "document"), allowingPath);
                }

                @ParameterizedTest
                @ValueSource(strings = {"object", "embed", "OBJECT", "EMBED"})
                void givenUnsafeDestination_thenRejected(String destination) {
                    var restrictedDest = new SecFetchSiteFilter(false, null, Set.of("navigate"), Set.of("iframe", "frame"), null);
                    assertRejected(exchange(HttpMethod.GET, "cross-site", "navigate", destination), restrictedDest);
                }

                @ParameterizedTest
                @ValueSource(strings = {"iframe", "FRAME"})
                void givenAllowedDestination_thenAllowed(String destination) {
                    var restrictedDest = new SecFetchSiteFilter(false, null, Set.of("navigate"), Set.of("iframe", "frame"), null);
                    assertAllowed(exchange(HttpMethod.GET, "cross-site", "navigate", destination), restrictedDest);
                }

                @Test
                void givenNullDestination_thenAllowed() {
                    assertAllowed(exchange(HttpMethod.GET, "cross-site", "navigate", null), filter);
                }
            }

            @Nested
            class WhenNonNavigateMode {

                @ParameterizedTest
                @ValueSource(strings = {"cors", "no-cors", "websocket"})
                @NullSource
                void thenRejected(String mode) {
                    assertRejected(exchange(HttpMethod.GET, "cross-site", mode, null), filter);
                }

                @Test
                void givenWebsocketModeConfiguredAsSafe_thenAllowed() {
                    var allowingWebsocket = new SecFetchSiteFilter(false, null, Set.of("navigate", "websocket"), null, null);
                    assertAllowed(exchange(HttpMethod.GET, "cross-site", "websocket", "websocket"), allowingWebsocket);
                }
            }

            @Test
            void givenNoSafeNavigationModes_thenRejected() {
                var noSafeModes = new SecFetchSiteFilter(false, null, null, null, null);
                assertRejected(exchange(HttpMethod.GET, "cross-site", "navigate", "document"), noSafeModes);
            }
        }
    }

}
