/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.web;

import ch.qos.logback.core.util.IpAddressMatcher;
import com.google.common.base.Objects;
import com.netflix.appinfo.InstanceInfo;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.zowe.apiml.message.log.ApimlLogger;

import java.net.IDN;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Builder
public class MetadataValidator {

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";
    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";
    private static final String ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED = "org.zowe.apiml.common.schemeNotAllowed";
    private static final String DEFAULT_PORT_TLS = "443";

    private final InstanceInfo instanceInfo;

    private final ApimlLogger apimlLogger;

    private final Set<String> allowedDomainsSet;

    private final boolean isClientAttlsEnabled;

    private final boolean disablePortValidation;

    boolean isAllowedDomain(String input, boolean validatePort) {
        if (StringUtils.isBlank(input)) {
            return true;
        }
        var inputToCheck = input.endsWith("null") ? input.substring(0, input.lastIndexOf("null")) : input;
        var result = allowedDomainsSet.stream().anyMatch(allowedDomain -> isAllowed(allowedDomain, inputToCheck, validatePort));
        if (!result && log.isDebugEnabled()) {
            log.debug("{} (validate port {}) is not allowed", input, validatePort);
        }
        return result;
    }

    boolean validateEntry(String label, String input, boolean validatePort) {
        if (StringUtils.isBlank(input)) {
            return true;
        }

        boolean isValid = true;

        if (!isAllowedDomain(input, validatePort)) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, label, input, instanceInfo.getInstanceId());
            isValid = false;
        }

        if (!isAllowedScheme(input)) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED, label, input, instanceInfo.getInstanceId());
            isValid = false;
        }

        if (isValid && log.isTraceEnabled()) {
            log.trace("URL {} is allowed for {}", input, label);
        }

        return isValid;
    }

    private boolean isAllowed(String allowedDomainEntry, String input, boolean validatePort) {
        log.debug("checking domain {} against allowed domain {}", input, allowedDomainEntry);

        var allowedDomainDomain = parseDomain(allowedDomainEntry);
        var allowedDomainPort = parsePort(allowedDomainEntry);

        input = input.toLowerCase();
        var domain = parseDomain(input);
        var result = false;
        if (StringUtils.isBlank(domain) || StringUtils.isBlank(allowedDomainDomain)) {
            result = false;
        } else if (domain.equals(allowedDomainDomain)) {
            result = true;
        } else if (isWildCard(allowedDomainDomain)) {
            result = isMatchingWildCard(domain, allowedDomainDomain);
        } else {
            result = isAllowedIpAddress(domain, allowedDomainDomain);
        }

        if (result && !disablePortValidation && validatePort) {
            result = isAllowedPort(input, allowedDomainPort);
        }

        return result;
    }

    /**
     * Parse port from an allowedDomain configuration entry
     *
     * @param input
     * @return the port or null
     */
    private String parsePort(String input) {
        if (IPAddressUtil.isIPV6Single(input)) {
            return IPAddressUtil.getPortIPV6(input);
        } else if (IPAddressUtil.isIPV6CIDR(input)) {
            return null;
        }
        var idx = input.lastIndexOf(":");
        if (idx > 0 && input.length() > idx) {
            return input.substring(idx + 1);
        }
        return null;
    }

    private String parseDomain(String input) {
        if (IPAddressUtil.isIPV6Single(input)) {
            return IPAddressUtil.getHostIPV6(input);
        } else if (IPAddressUtil.isIPV6CIDR(input)) {
            return input;
        } else if (IPAddressUtil.isIPV4CIDR(input)) {
            return input;
        }
        var noScheme = clearValidSchemes(input);
        input = StringUtils.isBlank(noScheme) ? input : noScheme;
        var idx = input.lastIndexOf(":");

        if (idx > 0) {
            input = input.substring(0, idx);
        }
        try {
            return new URL(Strings.CI.startsWithAny(input, HTTP, HTTPS) ? input : HTTPS + input).getHost().toLowerCase();
        } catch (MalformedURLException e) {
            // continue
        }
        try {
            return IDN.toASCII(input, IDN.ALLOW_UNASSIGNED);
        } catch (IllegalArgumentException e) {
            // continue
        }
        log.debug("{} is not a URL / hostname / IP Address", input);
        return null;
    }

    private String clearValidSchemes(String input) {
        if (Strings.CI.startsWith(input, HTTP)) {
            return StringUtils.substringAfter(input, HTTP);
        } else if (Strings.CI.startsWith(input, HTTPS)) {
            return StringUtils.substringAfter(input, HTTPS);
        }
        return input;
    }

    private boolean isAllowedScheme(String url) {
        if (StringUtils.isBlank(url)) {
            return true;
        }

        var scheme = getScheme(url);
        if (scheme != null) {
            return !"http".equals(scheme) || isClientAttlsEnabled;
        }

        return true;
    }

    private boolean isAllowedPort(String input, String allowedDomainPort) {
        var port = Integer.parseInt(extractPort(input));
        if ("*".equals(allowedDomainPort) || Objects.equal(String.valueOf(port), allowedDomainPort)) {
            return true;
        }
        log.debug("Port {} in input value {} from service {} does not match port {}", port, input, instanceInfo.getInstanceId(), allowedDomainPort);
        return false;
    }

    /**
     * Validate an input assumed to be an IP address (the method checks it as well)
     * against an entry in the allow list
     *
     * @param input IP address, yet to be validated input
     * @param allowedDomain An entry in the allowed domains list
     * @return whether the IP address is allowed against the allowed domain entry
     */
    private boolean isAllowedIpAddress(String input, String allowedDomain) {
        if (Objects.equal(input, allowedDomain)) {
            return true;
        }
        // first check for allowedDomain as a hostname
        InetAddress[] allowedDomainIps = null;
        String[] allowedDomainIpsString = null;
        if (isWildCard(allowedDomain)) {
            log.debug("{} is a wildcard match, can't evaluate against it against an IP address", allowedDomain);
            return false;
        }
        allowedDomainIps = IPAddressUtil.getIPAddresses(allowedDomain);
        if (allowedDomainIps != null && allowedDomainIps.length > 0) {
            allowedDomainIpsString = Arrays.stream(allowedDomainIps).map(InetAddress::getHostAddress).toList().toArray(new String[0]);
        } else {
            allowedDomainIpsString = new String[]{ allowedDomain };
        }

        try {
            for (String allowedIpAddress : allowedDomainIpsString) {
                var ipOrRange = new IpAddressMatcher(allowedIpAddress);
                if (ipOrRange.matches(input)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            log.debug("{} is not a valid IP Address or IP address range", allowedDomain);
            return false;
        }
    }

    private String extractPort(String input) {
        try {
            var port = new URL(Strings.CI.startsWithAny(input, HTTP, HTTPS) ? input : HTTPS + input).getPort();
            if (port > 0) {
                return String.valueOf(port);
            }
            return DEFAULT_PORT_TLS;
        } catch (MalformedURLException e) {
            return DEFAULT_PORT_TLS;
        }
    }

    private String getScheme(String url) {
        try {
            return new URL(url).toURI().getScheme().toLowerCase(Locale.ROOT);
        } catch (MalformedURLException | URISyntaxException e) {
            log.debug("'{}' is not a valid URL", url);
            return null;
        }
    }

    boolean isWildCard(String allowedDomain) {
        return Strings.CI.startsWith(allowedDomain, "*.");
    }

    boolean isMatchingWildCard(String domain, String wildCard) {
        return domain.endsWith(wildCard.substring(1));
    }

}
