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

import org.apache.commons.lang.StringUtils;

/**
 * This utilities allowe base work with cookies and its string representation.
 */
public final class CookieUtil {

    private CookieUtil() {
    }

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
        return sb.toString();
    }

}
