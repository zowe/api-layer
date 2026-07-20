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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Token-free CSRF protection based on the browser-supplied Fetch Metadata request headers
 * (specifically {@code Sec-Fetch-Site}). This complements, and does not replace, the existing
 * CORS handling and {@code SameSite} cookies.
 * <p>
 * {@code Sec-Fetch-*} headers are set by the browser and cannot be overridden by page scripts
 * (they are on the forbidden header name list), which is what makes them trustworthy for CSRF
 * defense. The policy applied here is a Fetch Metadata Resource Isolation Policy scoped to
 * state-changing requests:
 * <ul>
 *     <li>Safe methods (GET/HEAD/OPTIONS) are exempt - this covers top-level navigations and the
 *     CORS preflight (OPTIONS), and safe methods must not change state by HTTP semantics.</li>
 *     <li>A missing {@code Sec-Fetch-Site} header means a non-browser client (CLI, service-to-service)
 *     or a legacy browser - such a caller cannot be a CSRF vector, so the request is allowed.</li>
 *     <li>{@code same-origin} and {@code none} (user-initiated, e.g. typed URL or bookmark) are allowed.</li>
 *     <li>{@code same-site} and {@code cross-site} are allowed only when the request's {@code Origin}
 *     is permitted by the effective CORS configuration. This keeps the filter consistent with CORS:
 *     it becomes the enforcement arm of the same allow-list CORS only declares.</li>
 * </ul>
 * The CORS decision is delegated to the same live {@link CorsConfigurationSource} that backs the
 * Gateway's CORS filter, so both the Gateway default origins and the per-service origins declared in
 * Eureka registration metadata are honored automatically, including services that (de)register at runtime.
 */
@Slf4j
public class SecFetchSiteFilter implements WebFilter, Ordered {

    static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";

    private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);
    private static final Set<String> ALLOWED_SITES = Set.of("same-origin", "none");

    private final CorsConfigurationSource corsConfigurationSource;
    private final boolean enabled;

    public SecFetchSiteFilter(CorsConfigurationSource corsConfigurationSource, boolean enabled) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.enabled = enabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!enabled || isAllowed(exchange)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        log.debug("Rejecting request as a potential CSRF attempt: method={}, path={}, origin={}, Sec-Fetch-Site={}",
            request.getMethod(), request.getPath(), request.getHeaders().getOrigin(),
            request.getHeaders().getFirst(SEC_FETCH_SITE_HEADER));
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private boolean isAllowed(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        // Safe (non-state-changing) methods are exempt: navigations, cross-origin reads, CORS preflight.
        if (SAFE_METHODS.contains(request.getMethod())) {
            return true;
        }

        String site = request.getHeaders().getFirst(SEC_FETCH_SITE_HEADER);
        // Header absent -> non-browser client or legacy browser: cannot be a CSRF vector.
        if (site == null) {
            return true;
        }
        // Same-origin and user-initiated (typed URL / bookmark) requests are trusted.
        if (ALLOWED_SITES.contains(site)) {
            return true;
        }
        // same-site / cross-site: allow only if the Origin is on the effective CORS allow-list.
        return isAllowedByCors(exchange);
    }

    private boolean isAllowedByCors(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin == null) {
            return false;
        }
        CorsConfiguration corsConfiguration = corsConfigurationSource.getCorsConfiguration(exchange);
        return corsConfiguration != null && corsConfiguration.checkOrigin(origin) != null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
