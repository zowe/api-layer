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

    /**
     * Remove from parameter 'input' the first and the last occurrence of parameter 'str'
     * @param input
     * @param str
     * @return
     */
    public static String removeFirstAndLastOccurrence(String input, String str) {
        if (input == null) {
            return null;
        }

        input = input.trim();
        if (input.isEmpty()) {
            return "";
        }

        int start = 0, stop = input.length();

        if (input.startsWith(str)) {
            start = 1;
        }

        if (input.endsWith(str)) {
            stop = input.length() - 1;
        }

        input = input.substring(start, stop);

        return input;
    }

    /**
     * Prepend parameter 'subStr' to parameter 'input'
     *
     * @param uri
     * @param subStr
     * @return
     */
    public static String prependSubstring(String uri, String subStr) {
        return prependSubstring(uri, subStr, true);
    }

    /**
     * If 'input' is not already prefixed with 'subStr', prepend parameter 'subStr' to parameter 'input'
     *
     * @param uri
     * @param subStr
     * @param checkAlreadyPrepended
     * @return
     */
    public static String prependSubstring(String uri, String subStr, boolean checkAlreadyPrepended) {
        return prependSubstring(uri, subStr, checkAlreadyPrepended, true);
    }

    /**
     * If 'input' is not already prefixed with 'subStr', prepend parameter 'subStr' to parameter 'input'.
     * If 'input' has leading or trailing whitespaces, trim them first if 'shouldTrimWhitespaceFirst' is true
     */
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

    /**
     * If 'input' ends with 'subStr', remove this occurrence of 'subStr' only
     *
     * @param input
     * @param subStr
     * @return
     */
    public static String removeLastOccurrence(String input, String subStr) {
        if (input == null) {
            return null;
        }

        input = input.trim();
        if (input.isEmpty()) {
            return "";
        }

        if (input.endsWith(subStr)) {
            input = input.substring(0, input.length() - 1);
        }

        return input;
    }
}
