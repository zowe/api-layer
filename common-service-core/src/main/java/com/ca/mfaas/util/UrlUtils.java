/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;

import java.net.*;
import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class UrlUtils {

    private static Supplier<String> messageSupplier = new Supplier() {
        public String get() { return "Invalid URL"; }
    };


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
     * Substitute '\\W' with '-' in the input string and return the result
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
     * Checks validity of URL string.
     * Invalid URL string will trigger throwing a InvalidParameterException encapsulating the message of originally thrown MalformedURLException
     *
     * @param url
     */
    public static void validateUrl(String url) throws MalformedURLException {
        validateUrl(url, messageSupplier);
    }

    /**
     * Checks validity of URL string.
     * Invalid URL string will trigger throwing a InvalidParameterException encapsulating the message of originally thrown MalformedURLException
     *
     * @param url
     * @param exceptionSupplier
     */
    public static void validateUrl(String url, Supplier<String> exceptionSupplier) throws MalformedURLException {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new MalformedURLException(exceptionSupplier.get() + ": " + e.getMessage());
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

    /**
     * Finds all hostname IP addresses and returns the first one.
     * @param hostName
     * @return
     */
    public static String getHostFirstIPAddress(String hostName) {
        String ipAddr = null;
        try {
            InetAddress address = InetAddress.getByName(hostName);
            if (address != null ) {
                ipAddr = address.getHostAddress();
            }
        } catch (UnknownHostException e) {
            log.warn("InetAddress couldn't get byName: " + hostName, e);
        }

        return ipAddr;
    }
}
