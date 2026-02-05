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
     * Determines if a given string is an IPv6 address.
     *
     * @param address The string to check
     * @return true if the address is an IPv6 address, false otherwise
     */
    private boolean isIPv6Address(String address) {
        try {
            return InetAddress.getByName(address) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Validates if a string represents a valid port number.
     *
     * @param port The string to validate as a port number
     * @return true if the string represents a valid port, false otherwise
     */
    private boolean isValidPort(String port) {

        if (port == null || port.isEmpty()) {
            return false;
        }

        try {
            int portNum = Integer.parseInt(port);
            return portNum >= 0 && portNum <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Formats a hostname properly, ensuring IPv6 addresses are enclosed in square brackets.
     * If the input is already a properly formatted IPv6 address (with brackets), it remains unchanged.
     * Handles both IPv6 addresses and hostname:port combinations.
     *
     * @param hostname The hostname or IP address to format
     * @return Properly formatted hostname, with IPv6 addresses enclosed in square brackets
     */
    public String formatHostnameForUrl(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return hostname;
        }

        // If already properly formatted with brackets, return as is
        if (hostname.startsWith("[") && hostname.contains("]")) {
            return hostname;
        }

        // FIRST: Check if the ENTIRE string is a valid IPv6 address
        // This must be done BEFORE attempting to split by colon
        // because IPv6 addresses contain colons as part of the address
        if (isIPv6Address(hostname)) {
            return "[" + hostname + "]";
        }

        // Not a pure IPv6 address - check for hostname:port format
        // by looking at the last colon
        int lastColonIndex = hostname.lastIndexOf(':');
        if (lastColonIndex > -1) {
            String possibleHost = hostname.substring(0, lastColonIndex);
            String possiblePort = hostname.substring(lastColonIndex + 1);

            // Check if what follows the last colon is a valid port number
            if (isValidPort(possiblePort)) {
                // If we have a valid port, check if the host part is IPv6
                if (isIPv6Address(possibleHost)) {
                    return "[" + possibleHost + "]:" + possiblePort;
                }
                // Not IPv6, return as-is (hostname:port or IPv4:port)
                return hostname;
            }
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
        if (scheme == null || scheme.isEmpty()) {
        throw new IllegalArgumentException("Scheme cannot be null or empty");
        }

        if (hostWithPort == null || hostWithPort.isEmpty()) {
        throw new IllegalArgumentException("Host cannot be null or empty");
        }

        // Remove any existing scheme if present
        String cleanHostWithPort = hostWithPort.replaceFirst("^[a-zA-Z][a-zA-Z0-9+.-]*://", "");

        // Format the hostname part properly
        String formattedHost = formatHostnameForUrl(cleanHostWithPort);

        return String.format("%s://%s", scheme, formattedHost);
    }

    /**
     * Formats a URL string to ensure IPv6 addresses are properly bracketed.
     * <p>
     * For example, if a URL is constructed as "https://2001:db8::1:8080/path",
     * this method will convert it to "https://[2001:db8::1]:8080/path".
     *
     * @param urlString The URL string that may contain an un-bracketed IPv6 address
     * @return A properly formatted URL with IPv6 addresses enclosed in brackets,
     *         or the original string if it's not a valid URL or doesn't need formatting
     */
    public String formatUrlWithIPv6Support(String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return urlString;
        }

        if (urlString.contains("[") && urlString.contains("]")) {
            return urlString;
        }

        int schemeEnd = urlString.indexOf("://");
        if (schemeEnd == -1) {
            return urlString; // Not a URL with scheme
        }

        String scheme = urlString.substring(0, schemeEnd);
        String rest = urlString.substring(schemeEnd + 3); // Skip "://"

        // Find where the host:port ends (first slash or end of string)
        int pathStart = rest.indexOf('/');
        String hostPort;
        String pathAndQuery;
        if (pathStart == -1) {
            hostPort = rest;
            pathAndQuery = "";
        } else {
            hostPort = rest.substring(0, pathStart);
            pathAndQuery = rest.substring(pathStart);
        }

        // Check if hostPort contains an IPv6 address (multiple colons without brackets)
        long colonCount = hostPort.chars().filter(ch -> ch == ':').count();
        if (colonCount > 1) {
            // Find the port by looking for the last segment that's a valid port number
            int lastColon = hostPort.lastIndexOf(':');
            if (lastColon > 0) {
                String possiblePort = hostPort.substring(lastColon + 1);
                if (isValidPort(possiblePort)) {
                    // Everything before the last colon is the IPv6 address
                    String ipv6Address = hostPort.substring(0, lastColon);
                    if (isIPv6Address(ipv6Address)) {
                        return scheme + "://[" + ipv6Address + "]:" + possiblePort + pathAndQuery;
                    }
                }
            }
            // If no valid port found, the entire hostPort might be an IPv6 address
            if (isIPv6Address(hostPort)) {
                return scheme + "://[" + hostPort + "]" + pathAndQuery;
            }
        }

        return urlString;
    }
}
