/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

/**
 * A utility class for Cookies administration
 */
@UtilityClass
public final class CookieUtil {

    public static final String COOKIE_HEADER = "cookie";

    /**
     * It replace or add cookie into header string value (see header with name "Cookie").
     *
     * @param cookieHeader original header string value
     * @param name name of cookie to add or replace
     * @param value value of cookie to add or replace
     * @return new header string, which contains a new cookie
     */
    public static String setCookie(String cookieHeader, String name, String value) {
        StringBuilder sb = new StringBuilder();

        int counter = 0;
        if (StringUtils.isNotBlank(cookieHeader)) {
            for (final String cookie : cookieHeader.split(";")) {
                final String[] cookieParts = cookie.split("=", 2);
                if (StringUtils.equals(StringUtils.trim(cookieParts[0]), name)) continue;

                if (counter++ > 0) sb.append(';');
                sb.append(cookie);
            }
        }

        if (counter > 0) sb.append(';');
        sb.append(name).append('=').append(value);

        return sb.toString();
    }

    /**
     * It remove cookie from header value string (see header with name "Cookie") if exists. In case of missing
     * cookie, it return original header value string.
     *
     * @param cookieHeader original header string value
     * @param name name of cookie to remove
     * @return new header string, without a specified cookie
     */
    public static String removeCookie(String cookieHeader, String name) {
        StringBuilder sb = new StringBuilder();

        int counter = 0;
        boolean changed = false;
        if (StringUtils.isNotBlank(cookieHeader)) {
            for (final String cookie : cookieHeader.split(";")) {
                final String[] cookieParts = cookie.split("=", 2);
                if (StringUtils.equals(StringUtils.trim(cookieParts[0]), name)) {
                    changed = true;
                    continue;
                }

                if (counter++ > 0) sb.append(';');
                sb.append(cookie);
            }
        }

        if (!changed) return cookieHeader;
        return (sb.length() > 0) ? sb.toString() : null;
    }

}
