/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Token-free CSRF protection decision based on the browser-supplied Fetch Metadata request headers
 * (specifically {@code Sec-Fetch-Site}). This complements, and does not replace, the existing
 * CORS handling and {@code SameSite} cookies.
 * <p>
 * Shared between the HTTP Zuul pipeline ({@link SecFetchSiteFilter}) and the WebSocket proxy
 * handshake ({@code SecFetchSiteHandshakeInterceptor}) so both are judged consistently.
 * <ul>
 *     <li>A missing {@code Sec-Fetch-Site} header (non-browser/legacy client) or a value of
 *     {@code same-origin} / {@code none} continues.</li>
 *     <li>Otherwise ({@code same-site}, {@code cross-site} or any other value): the request continues
 *     only when the origin has already been validated by the CORS layer (see
 *     {@link #isHandledByCors(HttpServletRequest)}) or when it is a safe top-level navigation (see
 *     {@link #isSafeTopLevelNavigation(UnaryOperator, HttpServletRequest)}). Everything else is
 *     rejected.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecFetchSitePolicy {

    public static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    public static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";
    public static final String SEC_FETCH_DEST_HEADER = "Sec-Fetch-Dest";
    public static final String REJECTION_MESSAGE = "Access denied.";

    private static final Set<String> SAFE_SEC_FETCH_SITE_VALUES = new HashSet<>(Arrays.asList(
        "same-origin",
        "none"
    ));

    /**
     * Methods that cannot change state on their own, hence are safe to allow for a cross-site
     * top-level navigation (a user following a link to the gateway).
     */
    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList(
        "GET",
        "HEAD"
    ));

    private static final RequestMatcher MATCHES_NOTHING = request -> false;

    @Value("${security.secFetch.safeNavigationDestinations:#{null}}")
    private final Set<String> safeNavigationDestinations;

    /**
     * Optional - the bean is only defined for the new filter chain configuration (see {@code CorsBeans}),
     * so it must never be resolved eagerly.
     */
    private final ObjectProvider<CorsConfigurationSource> corsConfigurationSource;

    /**
     * The {@code Sec-Fetch-Mode} values that count as a top-level navigation. Configurable so that a
     * deployment can narrow it, or widen it to for example {@code websocket} - which is the only way to
     * accept a cross-site WebSocket handshake, as the handshake has no path to match against
     * {@code crossSiteNavigationAntMatchers}. An empty list rejects every cross-site request that CORS
     * has not already validated. Values are compared case-insensitively.
     */
    @Value("${security.secFetch.safeNavigationModes:navigate,same-origin}")
    private Set<String> safeNavigationModes;

    @Value("${security.secFetch.crossSiteNavigationAntMatchers:#{null}}")
    private String[] crossSiteNavigationAntMatchers;

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;

    @Value("${security.secFetch.enabled:true}")
    private boolean secFetchEnabled;

    private RequestMatcher crossSiteNavigationMatcher = MATCHES_NOTHING;

    @PostConstruct
    void init() {
        safeNavigationModes = toLowerCase(safeNavigationModes);
        initCrossSiteNavigationMatcher();
    }

    private static Set<String> toLowerCase(@Nullable Set<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }
        return values.stream()
            .map(value -> value.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
    }

    /**
     * The matchers are built once, on startup - {@link AntPathRequestMatcher} instances are stateless
     * and rebuilding them per request would put needless allocation on the proxying hot path.
     */
    private void initCrossSiteNavigationMatcher() {
        if (crossSiteNavigationAntMatchers == null || crossSiteNavigationAntMatchers.length == 0) {
            crossSiteNavigationMatcher = MATCHES_NOTHING;
            return;
        }

        List<RequestMatcher> matchers = Arrays.stream(crossSiteNavigationAntMatchers)
            .map(AntPathRequestMatcher::antMatcher)
            .collect(Collectors.toList());
        crossSiteNavigationMatcher = new OrRequestMatcher(matchers);

        log.info("Cross-site navigation with an unsafe HTTP method is allowed for: {}",
            Arrays.toString(crossSiteNavigationAntMatchers));
    }

    /**
     * For callers that have no {@link HttpServletRequest} available, such as the WebSocket handshake.
     * The path based rules cannot be evaluated then, so {@code crossSiteNavigationAntMatchers} never
     * matches such a request.
     */
    public boolean isAllowed(UnaryOperator<String> headerLookup) {
        return isAllowed(headerLookup, null);
    }

    public boolean isAllowed(UnaryOperator<String> headerLookup, @Nullable HttpServletRequest request) {
        if (!secFetchEnabled) {
            return true;
        }

        String secFetchSite = headerLookup.apply(SEC_FETCH_SITE_HEADER);

        // Absent header (non-browser/legacy client) or same-origin/none: continue.
        if (secFetchSite == null || SAFE_SEC_FETCH_SITE_VALUES.contains(secFetchSite.toLowerCase(Locale.ROOT))) {
            return true;
        }

        return isHandledByCors(request) || isSafeTopLevelNavigation(headerLookup, request);
    }

    /**
     * Whether the decision can safely be deferred to the CORS layer, which is only the case when all of
     * <ul>
     *     <li>the gateway's own CORS handling is enabled - when it is disabled a configuration allowing
     *     no origin at all is registered for every path (see {@code CorsUtils}), which validates nothing,</li>
     *     <li>the request carries an {@code Origin} - without it the request is not a CORS request and the
     *     {@code DefaultCorsProcessor} is not applied, and</li>
     *     <li>a {@link CorsConfiguration} is registered for the requested path - otherwise
     *     {@code CorsFilter} lets the request through without looking at its origin.</li>
     * </ul>
     * hold. Then {@code CorsFilter} has already rejected every origin that is not allow-listed before the
     * request reached the Zuul pipeline, so there is nothing left for this policy to decide.
     */
    boolean isHandledByCors(@Nullable HttpServletRequest request) {
        if (!corsEnabled || request == null || request.getHeader(HttpHeaders.ORIGIN) == null) {
            return false;
        }

        CorsConfigurationSource configurationSource = corsConfigurationSource.getIfAvailable();
        if (configurationSource == null) {
            log.debug("CORS is enabled but no CorsConfigurationSource is available, cannot defer to CORS.");
            return false;
        }

        return configurationSource.getCorsConfiguration(request) != null;
    }

    private boolean isSafeTopLevelNavigation(UnaryOperator<String> headerLookup, @Nullable HttpServletRequest request) {
        String mode = headerLookup.apply(SEC_FETCH_MODE_HEADER);
        if (mode == null || !safeNavigationModes.contains(mode.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!isSafeMethod(request) && !crossSiteNavigationAllowed(request)) {
            return false;
        }
        String dest = headerLookup.apply(SEC_FETCH_DEST_HEADER);
        return isSafeDest(dest);
    }

    /**
     * A cross-site navigation performed with a state-changing method - a form auto-submitted by an
     * attacker's page - is the classic CSRF vector, so it is only allowed for the paths explicitly
     * listed in {@code security.secFetch.crossSiteNavigationAntMatchers}.
     */
    private boolean isSafeMethod(@Nullable HttpServletRequest request) {
        // No request available: the only such caller is the WebSocket handshake, which RFC 6455 requires
        // to be a GET. Whether it is accepted at all is decided by 'websocket' being a safe mode or not.
        if (request == null) {
            return true;
        }
        return request.getMethod() != null
            && SAFE_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT));
    }

    private boolean crossSiteNavigationAllowed(@Nullable HttpServletRequest request) {
        return request != null && crossSiteNavigationMatcher.matches(request);
    }

    private boolean isSafeDest(String dest) {
        // No destinations configured: nothing to restrict against, allow.
        return dest == null
            || safeNavigationDestinations == null
            || safeNavigationDestinations.isEmpty()
            || safeNavigationDestinations.contains(dest.toLowerCase(Locale.ROOT));
    }

    /**
     * Whether the request is cross-site per the {@code Sec-Fetch-Site} header, i.e. the header is present
     * and not one of the safe values. Used to decide whether {@code Origin} needs to be forwarded to the
     * southbound service (which the gateway otherwise strips - see {@code CorsBeans}) for it to apply its
     * own CORS/Fetch-Metadata handling.
     */
    public boolean isCrossSite(UnaryOperator<String> headerLookup) {
        String secFetchSite = headerLookup.apply(SEC_FETCH_SITE_HEADER);
        return secFetchSite != null && !SAFE_SEC_FETCH_SITE_VALUES.contains(secFetchSite.toLowerCase(Locale.ROOT));
    }

}
