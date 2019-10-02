/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringUtils {

    public static String removeFirstAndLastOccurrence(String uri, String str) {
        if (uri == null) {
            return null;
        }

        uri = uri.trim();
        if (uri.isEmpty()) {
            return "";
        }

        int start = 0, stop = uri.length();

        if (uri.startsWith(str)) {
            start = 1;
        }

        if (uri.endsWith(str)) {
            stop = uri.length() - 1;
        }

        uri = uri.substring(start, stop);

        return uri;
    }

    public static String prependSubstring(String uri, String subStr) {
        return prependSubstring(uri, subStr, true);
    }

    public static String prependSubstring(String uri, String subStr, boolean checkAlreadyPrepended) {
        return prependSubstring(uri, subStr, checkAlreadyPrepended, true);
    }

    public static String prependSubstring(String uri, String subStr, boolean checkAlreadyPrepended, boolean shouldTrimWhitespaceFirst) {
        if (uri == null) {
            return null;
        }

        if (shouldTrimWhitespaceFirst) {
            uri = uri.trim();
        }

        if (!checkAlreadyPrepended || !uri.startsWith(subStr)) {
            uri = subStr + uri;
        }

        return uri;
    }

    public static String removeLastOccurrence(String uri, String subStr) {
        if (uri == null) {
            return null;
        }

        uri = uri.trim();
        if (uri.isEmpty()) {
            return "";
        }

        if (uri.endsWith(subStr)) {
            uri = uri.substring(0, uri.length() - 1);
        }

        return uri;
    }
}
