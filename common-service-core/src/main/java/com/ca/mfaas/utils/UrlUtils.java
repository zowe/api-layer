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
import org.apache.commons.lang.RandomStringUtils;

@UtilityClass
public class UrlUtils {

    /**
     * Remove slashes from input string parameter
     *
     * @param string
     * @return
     */
    public static String trimSlashes(String string) {
        return string.replaceAll("^/|/$", "");
    }

    /**
     * Substitute '\\' with '-' in the input string and return the result
     *
     * @param url
     * @return
     */
    public static String getEncodedUrl(String url) {
        if (url != null) {
            return url.replaceAll("\\W", "-");
        } else {
            return RandomStringUtils.randomAlphanumeric(10);
        }
    }

    /**
     * Removes leading and trailing slashes ("/") from input string
     * @param uri
     * @return
     */
    public static String removeFirstAndLastSlash(String uri) {
        return StringUtils.removeFirstAndLastOccurrence(uri, "/");
    }

    /**
     * Prepends a slash ("/") to input string
     * @param uri
     * @return
     */
    public static String addFirstSlash(String uri) {
        return StringUtils.prependSubstring(uri, "/");
    }

    /**
     * Removes laast slash ("/") from input string
     * @param uri
     * @return
     */
    public static String removeLastSlash(String uri) {
        return StringUtils.removeLastOccurrence(uri, "/");
    }

}
