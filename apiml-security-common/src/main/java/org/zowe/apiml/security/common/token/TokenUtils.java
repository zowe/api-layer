/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.token;

import org.springframework.http.HttpHeaders;
import org.zowe.apiml.constants.ApimlConstants;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

public class TokenUtils {

    /**
     * Get the JWT token from the authorization header in the http request
     * <p>
     * Order:
     * 1. Cookie
     * 2. Authorization header
     *
     * @param request the http request
     * @return the JWT token
     */
    public static Optional<String> getJwtTokenFromRequest(HttpServletRequest request, String cookieName) {
        Optional<String> fromCookie = getJwtTokenFromCookie(request, cookieName);
        if (!fromCookie.isPresent()) {
            return extractJwtTokenFromAuthorizationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        }
        return fromCookie;
    }

    private static Optional<String> getJwtTokenFromCookie(HttpServletRequest request, final String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(cookieName))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(Cookie::getValue);
    }

    /**
     * Extract the JWT token from the authorization header
     *
     * @param header the http request header
     * @return the JWT token
     */
    private static Optional<String> extractJwtTokenFromAuthorizationHeader(String header) {
        if (header != null && header.startsWith(ApimlConstants.BEARER_AUTHENTICATION_PREFIX)) {
            header = header.replaceFirst(ApimlConstants.BEARER_AUTHENTICATION_PREFIX, "").trim();
            if (header.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(header);
        }

        return Optional.empty();
    }
}
