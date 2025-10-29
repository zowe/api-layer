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

import java.net.*;
import java.security.SecureRandom;
import java.util.Arrays;

@UtilityClass
public class UrlUtils {


    /**
     * Remove slashes from input string parameter
     *
     * @param string input parameter
     * @return input without removed trailing slashes.
     */
    public String trimSlashes(String string) {
        return string.replaceAll("((^/)|(/$))", "");
    }

    /**
     * Substitute '\\W' with '-' in the input string and return the result
     *
     * @param url - input url to be encoded by the hard coded character substitution
     * @return An url string with any non alpha-numeric characters substituted by '-'
     */
    public String getEncodedUrl(String url) {
        if (url != null) {
            return url.replaceAll("\\W", "-");
        } else {
            byte[] bytes = new byte[20];
            new SecureRandom().nextBytes(bytes);
            return Arrays.toString(bytes);
        }
    }

    /**
     * Removes leading and trailing slashes ("/") from input string
     *
     * @param uri an URI string to trim slashes from
     * @return the trimmed URI string
     */
    public String removeFirstAndLastSlash(String uri) {
        return StringUtils.removeFirstAndLastOccurrence(uri, "/");
    }

    /**
     * Prepends a slash ("/") to input string
     *
     * @param uri An URI to prepend a '/' to.
     * @return the modified URI string
     */
    public String addFirstSlash(String uri) {
        return StringUtils.prependSubstring(uri, "/");
    }

    /**
     * Removes last slash ("/") from input string
     * @param uri an URI string to trim last slash from
     * @return the modified URI
     */
    public String removeLastSlash(String uri) {
        return StringUtils.removeLastOccurrence(uri, "/");
    }

    /**
     * Finds IP address hostname provided by fqdn string.
     *
     * @param fqdn a Fully Qualified Domain Name to resolve as IP address
     * @return the resolved IP address or 'null'
     */
    public String getHostIPAddress(String fqdn) throws UnknownHostException {
        return InetAddress.getByName(fqdn).getHostAddress();
    }

    /**
     *
     * @param urlString is a string representing a URL
     * @return IP address of the host domain name provided by FQDN
     * @throws MalformedURLException if urlString parameter is not valid URL
     * @throws UnknownHostException if host name part of the URL is not resolvable
     */
    public String getIpAddressFromUrl(String urlString) throws MalformedURLException, UnknownHostException {
        URL baseUrl = new URL(urlString);

        String hostname = baseUrl.getHost();
        return UrlUtils.getHostIPAddress(hostname);
    }

    /**
     *
     * @param urlString is a string representing a URL
     * @return true if provided string is actually valid URL format. False otherwise
     */
    public boolean isValidUrl(String urlString) {
        try {
            new URL(urlString).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    /**
     * Formats a hostname properly, ensuring IPv6 addresses are enclosed in square brackets.
     * If the input is already a properly formatted IPv6 address (with brackets), it remains unchanged.
     *
     * @param hostname The hostname or IP address to format
     * @return Properly formatted hostname, with IPv6 addresses enclosed in square brackets
     */
    public String formatHostnameForUrl(String hostname) {
        if (hostname == null) return null;

        // If hostname already has IPv6 brackets, don't add them again
        if (hostname.startsWith("[") && hostname.contains("]")) {
            return hostname;
        }

        // Check if this is an IPv6 address (contains multiple colons)
        if (hostname.contains(":") && !hostname.startsWith("[")) {
            return "[" + hostname + "]";
        }

        return hostname;
    }

    /**
     * Creates a proper URL string with scheme, hostname, and port,
     * handling IPv6 addresses correctly.
     *
     * @param scheme The URL scheme (http, https, etc.)
     * @param hostname The hostname or IP address
     * @param port The port number
     * @return A properly formatted URL string with IPv6 address handling
     */
    public String getUrl(String scheme, String hostname, int port) {
        String formattedHostname = formatHostnameForUrl(hostname);
        return String.format("%s://%s:%d", scheme, formattedHostname, port);
    }

    /**
     * Creates a proper URL string with scheme and host (which may include port),
     * handling IPv6 addresses correctly.
     *
     * @param scheme The URL scheme (http, https, etc.)
     * @param hostWithPort The hostname or IP address, possibly including a port
     * @return A properly formatted URL string with IPv6 address handling
     */
    public String getUrl(String scheme, String hostWithPort) {
        // If host already includes port
        if (hostWithPort.contains(":") && !hostWithPort.endsWith("]")) {
            // Handle IPv6 address with port
            if (hostWithPort.contains("]:")) {
                // Already properly formatted IPv6 with port
                return String.format("%s://%s", scheme, hostWithPort);
            } else if (hostWithPort.contains("[") && hostWithPort.contains("]")) {
                // IPv6 without port, just wrap in scheme
                return String.format("%s://%s", scheme, hostWithPort);
            } else {
                // Might be IPv6 without brackets or IPv4 with port
                int lastColonIndex = hostWithPort.lastIndexOf(':');
                if (hostWithPort.substring(0, lastColonIndex).contains(":")) {
                    // It's an IPv6 address with port, but without brackets
                    String host = hostWithPort.substring(0, lastColonIndex);
                    String port = hostWithPort.substring(lastColonIndex);
                    return String.format("%s://[%s]%s", scheme, host, port);
                }
                // IPv4 with port, no special handling needed
                return String.format("%s://%s", scheme, hostWithPort);
            }
        }

        // just hostname without port
        return String.format("%s://%s", scheme, formatHostnameForUrl(hostWithPort));
    }
}
