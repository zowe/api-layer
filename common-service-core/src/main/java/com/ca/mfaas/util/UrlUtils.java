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

@Slf4j
@UtilityClass
public class UrlUtils {


    /**
     * Remove slashes from input string parameter
     *
     * @param string input parameter
     * @return input without removed trailing slashes.
     */
    public static String trimSlashes(String string) {
        return string.replaceAll("^/|/$", "");
    }

    /**
     * Substitute '\\W' with '-' in the input string and return the result
     *
     * @param url - input url to be encoded by the hard coded character substitution
     * @return An url string with any non alpha-numeric characters substituted by '-'
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
     *
     * @param uri an URI string to trim slashes from
     * @return the trimmed URI string
     */
    public static String removeFirstAndLastSlash(String uri) {
        return StringUtils.removeFirstAndLastOccurrence(uri, "/");
    }

    /**
     * Prepends a slash ("/") to input string
     *
     * @param uri An URI to prepend a '/' to.
     * @return the modified URI string
     */
    public static String addFirstSlash(String uri) {
        return StringUtils.prependSubstring(uri, "/");
    }

    /**
     * Removes last slash ("/") from input string
     * @param uri an URI string to trim last slash from
     * @return the modified URI
     */
    public static String removeLastSlash(String uri) {
        return StringUtils.removeLastOccurrence(uri, "/");
    }

    /**
     * Finds IP address hostname provided by fqdn string.
     *
     * @param fqdn a Fully Qualified Domain Name to resolve as IP address
     * @return the resolved IP address or 'null'
     */
    public static String getHostIPAddress(String fqdn) throws UnknownHostException {
        return InetAddress.getByName(fqdn).getHostAddress();
    }

    /**
     *
     * @param urlString is a string representing a URL
     * @return IP address of the host domain name provided by FQDN
     * @throws MalformedURLException
     * @throws UnknownHostException
     */
    public static String getIpAddressFromUrl(String urlString) throws MalformedURLException, UnknownHostException {
        URL baseUrl = new URL(urlString);

        String hostname = baseUrl.getHost();
        return UrlUtils.getHostIPAddress(hostname);
    }

}
