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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Token-free CSRF protection decision based on the browser-supplied Fetch Metadata request headers
 * (specifically {@code Sec-Fetch-Site}). This complements, and does not replace, the existing
 * CORS handling and {@code SameSite} cookies.
 * <p>
 * Shared between the HTTP Zuul pipeline ({@link SecFetchSiteFilter}) and the WebSocket proxy
 * handshake ({@code SecFetchSiteHandshakeInterceptor}) so both are judged consistently.
 * <ul>
 *     <li>A missing {@code Sec-Fetch-Site} header (non-browser/legacy client) or a value of
 *     {@code same-origin} / {@code same-site} / {@code none} continues.</li>
 *     <li>Otherwise ({@code cross-site} or any other value): when CORS is enabled the request's
 *     {@code Origin} is validated (and rejected if not permitted) by the CORS {@code DefaultCorsProcessor},
 *     so the decision is deferred to CORS; when CORS is disabled only a safe top-level navigation is
 *     allowed and everything else is rejected.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SecFetchSitePolicy {

    public static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    public static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";
    public static final String SEC_FETCH_DEST_HEADER = "Sec-Fetch-Dest";
    public static final String REJECTION_MESSAGE = "Access denied.";

    private static final Set<String> SAFE_SEC_FETCH_SITE_VALUES = new HashSet<>(Arrays.asList(
        "same-origin",
        "same-site",
        "none"
    ));

    private static final Set<String> SAFE_MODE = new HashSet<>(Arrays.asList(
        "navigate",
        "same-origin",
        "websocket"
    ));

    @Value("${security.secFetch.safeNavigationDestinations:#{null}}")
    private final Set<String> safeNavigationDestinations;
    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;

    @Value("${security.secFetch.enabled:true}")
    private boolean secFetchEnabled;

    public boolean isAllowed(UnaryOperator<String> headerLookup) {
        if (!secFetchEnabled) {
            return true;
        }

        String secFetchSite = headerLookup.apply(SEC_FETCH_SITE_HEADER);

        // Absent header (non-browser/legacy client) or same-origin/same-site/none: continue.
        if (secFetchSite == null || SAFE_SEC_FETCH_SITE_VALUES.contains(secFetchSite.toLowerCase(Locale.ROOT))) {
            return true;
        }

        return corsEnabled || isSafeTopLevelNavigation(headerLookup);
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

    private boolean isSafeTopLevelNavigation(UnaryOperator<String> headerLookup) {
        String mode = headerLookup.apply(SEC_FETCH_MODE_HEADER);
        if (mode == null || !SAFE_MODE.contains(mode.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String dest = headerLookup.apply(SEC_FETCH_DEST_HEADER);
        return isSafeDest(dest);
    }

    private boolean isSafeDest(String dest) {
        // No destinations configured: nothing to restrict against, allow.
        return dest == null
            || safeNavigationDestinations == null
            || safeNavigationDestinations.isEmpty()
            || safeNavigationDestinations.contains(dest.toLowerCase(Locale.ROOT));
    }

}
