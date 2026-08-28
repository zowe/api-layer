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

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Token-free CSRF protection decision based on the browser-supplied Fetch Metadata request headers
 * (specifically {@code Sec-Fetch-Site}). This complements, and does not replace, the existing CORS
 * handling and {@code SameSite} cookies. Since every proxied request - including the WebSocket
 * handshake, which is an HTTP GET upgrade - passes through the {@link WebFilter} chain, this single
 * filter judges the whole Gateway edge consistently.
 * <ul>
 *     <li>A missing {@code Sec-Fetch-Site} header (non-browser/legacy client) or a value of
 *     {@code same-origin} / {@code none} continues.</li>
 *     <li>Otherwise ({@code same-site}, {@code cross-site} or any other value): the request continues
 *     only when the origin is validated by the CORS layer (see {@link #isHandledByCors(ServerWebExchange)})
 *     or when it is a safe top-level navigation (see {@link #isSafeTopLevelNavigation(ServerWebExchange)}).
 *     Everything else is rejected with 403.</li>
 * </ul>
 */
@Slf4j
public class SecFetchSiteFilter implements WebFilter, Ordered {

    static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";
    static final String SEC_FETCH_DEST_HEADER = "Sec-Fetch-Dest";

    private static final Set<String> SAFE_SEC_FETCH_SITE_VALUES = Set.of("same-origin", "none");

    /**
     * Methods that cannot change state on their own, hence are safe to allow for a cross-site
     * top-level navigation (a user following a link to the Gateway).
     */
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);

    private static final String REJECTION_MESSAGE = "Access denied.";

    private final boolean corsEnabled;

    /**
     * The live configuration source backing the CORS {@code WebFilter}, or {@code null} when none is
     * registered. It is queried, never mutated, to find out whether CORS judges the requested path.
     */
    private final CorsConfigurationSource corsConfigurationSource;

    /**
     * The {@code Sec-Fetch-Mode} values that count as a top-level navigation. Configurable so that a
     * deployment can narrow it, or widen it to accept cross-site WebSocket handshakes by adding
     * {@code websocket} - a handshake is a GET, so the mode is what decides it. An empty set rejects
     * every cross-site request that CORS does not validate. Values are compared case-insensitively.
     */
    private final Set<String> safeNavigationModes;

    private final Set<String> safeNavigationDestinations;

    public SecFetchSiteFilter(
        boolean corsEnabled,
        @Nullable CorsConfigurationSource corsConfigurationSource,
        @Nullable Collection<String> safeNavigationModes,
        @Nullable Collection<String> safeNavigationDestinations
    ) {
        this.corsEnabled = corsEnabled;
        this.corsConfigurationSource = corsConfigurationSource;
        this.safeNavigationModes = toLowerCase(safeNavigationModes);
        this.safeNavigationDestinations = toLowerCase(safeNavigationDestinations);
    }

    private static Set<String> toLowerCase(@Nullable Collection<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isAllowed(exchange)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        log.debug("Blocked request {} {} - Sec-Fetch-Site={}, Sec-Fetch-Mode={}, Sec-Fetch-Dest={}",
            request.getMethod(), request.getPath(), headers.getFirst(SEC_FETCH_SITE_HEADER),
            headers.getFirst(SEC_FETCH_MODE_HEADER), headers.getFirst(SEC_FETCH_DEST_HEADER));

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        DataBuffer body = response.bufferFactory().wrap(REJECTION_MESSAGE.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(body));
    }

    private boolean isAllowed(ServerWebExchange exchange) {
        String secFetchSite = exchange.getRequest().getHeaders().getFirst(SEC_FETCH_SITE_HEADER);

        // Absent header (non-browser/legacy client) or same-origin/none: continue.
        if (secFetchSite == null || SAFE_SEC_FETCH_SITE_VALUES.contains(secFetchSite.toLowerCase(Locale.ROOT))) {
            return true;
        }

        return isHandledByCors(exchange) || isSafeTopLevelNavigation(exchange);
    }

    /**
     * Whether the decision can safely be deferred to the CORS layer, which is only the case when all of
     * <ul>
     *     <li>the Gateway's own CORS handling is enabled - when it is disabled the configuration
     *     registered for every path allows no origin at all (see {@code CorsUtils}), so it carries no
     *     per-service intent that could be deferred to,</li>
     *     <li>the request carries an {@code Origin} - without it the request is not a CORS request and the
     *     {@code DefaultCorsProcessor} is not applied, and</li>
     *     <li>a {@link CorsConfiguration} is registered for the requested path - otherwise
     *     {@code CorsWebFilter} lets the request through without looking at its origin.</li>
     * </ul>
     * hold. Then {@code CorsWebFilter} rejects every origin that is not allow-listed for the requested
     * path, so there is nothing left for this filter to decide.
     */
    boolean isHandledByCors(ServerWebExchange exchange) {
        if (!corsEnabled || exchange.getRequest().getHeaders().getOrigin() == null) {
            return false;
        }

        if (corsConfigurationSource == null) {
            log.debug("CORS is enabled but no CorsConfigurationSource is available, cannot defer to CORS.");
            return false;
        }

        return corsConfigurationSource.getCorsConfiguration(exchange) != null;
    }

    private boolean isSafeTopLevelNavigation(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        String mode = request.getHeaders().getFirst(SEC_FETCH_MODE_HEADER);
        if (mode == null || !safeNavigationModes.contains(mode.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!isSafeMethod(request)) {
            return false;
        }
        return isSafeDest(request.getHeaders().getFirst(SEC_FETCH_DEST_HEADER));
    }

    private boolean isSafeMethod(ServerHttpRequest request) {
        return SAFE_METHODS.contains(request.getMethod());
    }

    private boolean isSafeDest(String dest) {
        return dest == null
            || safeNavigationDestinations.isEmpty()
            || safeNavigationDestinations.contains(dest.toLowerCase(Locale.ROOT));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
