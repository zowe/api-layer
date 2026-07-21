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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Token-free CSRF protection based on the browser-supplied Fetch Metadata request headers
 * (specifically {@code Sec-Fetch-Site}). This complements, and does not replace, the existing
 * CORS handling and {@code SameSite} cookies.
 * <p>
 * This is the reactive (Gateway edge) counterpart of the servlet {@code SecFetchSiteFilter} enforced
 * by southbound services; both apply the same Fetch Metadata Resource Isolation Policy so a request is
 * judged consistently at both tiers:
 * <ul>
 *     <li>A missing {@code Sec-Fetch-Site} header (non-browser/legacy client) or a value of
 *     {@code same-origin} / {@code same-site} / {@code none} continues.</li>
 *     <li>Otherwise ({@code cross-site} or any other value): when CORS is enabled the request's
 *     {@code Origin} is validated (and rejected if not permitted) by the CORS {@code DefaultCorsProcessor},
 *     so the decision is deferred to CORS; when CORS is disabled only a safe top-level navigation is
 *     allowed and everything else is rejected with 403.</li>
 * </ul>
 */
@Slf4j
public class SecFetchSiteFilter implements WebFilter, Ordered {

    static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";
    static final String SEC_FETCH_DEST_HEADER = "Sec-Fetch-Dest";

    private static final Set<String> SAFE_SEC_FETCH_SITE_VALUES = Set.of("same-origin", "same-site", "none");
    private static final String NAVIGATE_MODE = "navigate";
    private static final Set<HttpMethod> SAFE_NAVIGATION_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);
    private static final Set<String> UNSAFE_NAVIGATION_DESTINATIONS = Set.of("object", "embed");
    private static final String REJECTION_MESSAGE = "Invalid CORS request";

    private final boolean corsEnabled;

    public SecFetchSiteFilter(boolean corsEnabled) {
        this.corsEnabled = corsEnabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isAllowed(exchange)) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        log.debug("Blocked cross-site {} {} - Sec-Fetch-Site={}, CORS is not enabled",
            request.getMethod(), request.getPath(), request.getHeaders().getFirst(SEC_FETCH_SITE_HEADER));
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
        DataBuffer body = response.bufferFactory().wrap(REJECTION_MESSAGE.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(body));
    }

    private boolean isAllowed(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String secFetchSite = request.getHeaders().getFirst(SEC_FETCH_SITE_HEADER);

        // Absent header (non-browser/legacy client) or same-origin/same-site/none: continue.
        if (secFetchSite == null || SAFE_SEC_FETCH_SITE_VALUES.contains(secFetchSite.toLowerCase(Locale.ROOT))) {
            return true;
        }

        // Cross-site (or any other value): when CORS is enabled the Origin is validated by the CORS
        // DefaultCorsProcessor, so defer to it; otherwise allow only a safe top-level navigation.
        return corsEnabled || isSafeTopLevelNavigation(request);
    }

    /**
     * A safe top-level navigation per the Fetch Metadata Resource Isolation Policy: a
     * {@code Sec-Fetch-Mode: navigate} request using a safe method that is not being loaded into an
     * {@code <object>} or {@code <embed>}. Such navigations cannot read the response cross-origin
     * and, being read-only, cannot change server state, so they are allowed even when cross-site.
     */
    private boolean isSafeTopLevelNavigation(ServerHttpRequest request) {
        String mode = request.getHeaders().getFirst(SEC_FETCH_MODE_HEADER);
        if (!NAVIGATE_MODE.equalsIgnoreCase(mode)) {
            return false;
        }
        if (!SAFE_NAVIGATION_METHODS.contains(request.getMethod())) {
            return false;
        }
        String dest = request.getHeaders().getFirst(SEC_FETCH_DEST_HEADER);
        return dest == null || !UNSAFE_NAVIGATION_DESTINATIONS.contains(dest.toLowerCase(Locale.ROOT));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
