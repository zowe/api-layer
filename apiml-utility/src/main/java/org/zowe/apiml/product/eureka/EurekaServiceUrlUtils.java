/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper for embedding basic authentication credentials into Eureka discovery service URLs.
 * <p>
 * The Netflix Eureka client (used both by the onboarding enablers and the internal API ML services)
 * performs HTTP basic authentication only when the credentials are present in the service URL as
 * {@code scheme://userid:password@host:port/path}. When TLS validation is disabled
 * ({@code apiml.security.ssl.verifySslCertificatesOfServices=false}) the client certificate cannot be
 * trusted, so the Discovery Service requires basic authentication instead. This helper rewrites the
 * configured discovery URLs to carry the credentials in that situation.
 */
public final class EurekaServiceUrlUtils {

    private static final String SCHEME_SEPARATOR = "://";

    private EurekaServiceUrlUtils() {
    }

    /**
     * Embeds the given credentials into the authority part of the URL if they are not already present.
     *
     * @param url      the discovery service URL (e.g. {@code https://host:10011/eureka/})
     * @param userid   the eureka user id; if blank the URL is returned unchanged
     * @param password the eureka password; if blank the URL is returned unchanged
     * @return the URL with {@code userid:password@} inserted after the scheme, or the original URL when
     * the credentials are missing, the URL has no scheme, or it already contains user information
     */
    public static String addCredentials(String url, String userid, String password) {
        if (url == null || isBlank(userid) || isBlank(password)) {
            return url;
        }

        // Do not touch unresolved placeholders or SpEL expressions (e.g. the http-profile allPeersUrls
        // that already conditionally embeds credentials); only fully resolved URLs are rewritten.
        if (url.contains("{")) {
            return url;
        }

        int schemeIdx = url.indexOf(SCHEME_SEPARATOR);
        if (schemeIdx < 0) {
            return url;
        }

        int authorityStart = schemeIdx + SCHEME_SEPARATOR.length();
        int pathIdx = url.indexOf('/', authorityStart);
        String authority = (pathIdx < 0) ? url.substring(authorityStart) : url.substring(authorityStart, pathIdx);
        if (authority.contains("@")) {
            // URL already carries user information, keep it untouched
            return url;
        }

        return url.substring(0, authorityStart) + userid + ':' + password + '@' + url.substring(authorityStart);
    }

    /**
     * Applies {@link #addCredentials(String, String, String)} to every URL in the list.
     */
    public static List<String> addCredentials(List<String> urls, String userid, String password) {
        if (urls == null) {
            return urls;
        }
        return urls.stream()
            .map(url -> addCredentials(url, userid, password))
            .collect(Collectors.toList());
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
