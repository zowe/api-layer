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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Objects;
import com.netflix.appinfo.InstanceInfo;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.message.log.ApimlLogger;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Builder
public class MetadataValidator {

    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";
    private static final String ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED = "org.zowe.apiml.common.schemeNotAllowed";

    private static final Cache<String, InetAddress[]> domainToIpAddresses = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    private final InstanceInfo instanceInfo;

    private final ApimlLogger apimlLogger;

    private final Set<String> allowedDomainsSet;

    private final boolean isClientAttlsEnabled;

    boolean isAllowedDomain(String input) {
        if (StringUtils.isBlank(input)) {
            return true;
        }
        var inputToCheck = input.endsWith("null") ? input.substring(0, input.lastIndexOf("null")) : input;
        return allowedDomainsSet.stream().anyMatch(allowedDomain -> isAllowed(allowedDomain, inputToCheck));
    }

    boolean validateEntry(String label, String input) {
        if (StringUtils.isBlank(input)) {
            return true;
        }

        boolean isValid = true;

        if (!isAllowedDomain(input)) {
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

    private boolean isAllowed(String allowedDomainEntry, String input) {
        log.debug("checking domain {} against allowed domain {}", input, allowedDomainEntry);

        allowedDomainEntry = parseDomain(allowedDomainEntry);
        var allowedDomainPort = parsePort(allowedDomainEntry);

        input = input.toLowerCase();
        input = extractDomain(input);
        var result = false;
        if (input == null) {
            result = false;
        } else if (input.equals(allowedDomainEntry)) {
            result = true;
        } else if (isWildCard(allowedDomainEntry)) {
            result = isMatchingWildCard(input, allowedDomainEntry);
        } else {
            result = isAllowedIpAddress(input, allowedDomainEntry);
        }

        if (result) {
            result = isAllowedPort(input, allowedDomainPort);
        }

        return result;
    }

    private String parsePort(String allowedDomainEntry) {
        var idx = allowedDomainEntry.lastIndexOf(":");
        if (idx > 0) {
            return allowedDomainEntry.substring(0, idx);
        }
        return null;
    }

    private String parseDomain(String allowedDomainEntry) {
        var idx = allowedDomainEntry.lastIndexOf(":");
        if (idx > 0) {
            return allowedDomainEntry.substring(0, idx);
        }
        return allowedDomainEntry;
    }

    private String extractDomain(String input) {
        try {
            return new URL(input).getHost().toLowerCase();
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
        if ("*".equals(allowedDomainPort)) {
            return true;
        }
        return Objects.equal(input, allowedDomainPort);
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
            log.debug("{} is a wildcard match, can't evaluate against it against an IP address");
            return false;
        }
        allowedDomainIps = domainToIpAddresses.get(allowedDomain, this::getInetAddresses);
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

    private InetAddress[] getInetAddresses(String domain) {
        try {
            var addresses = InetAddress.getAllByName(domain);
            if (log.isDebugEnabled()) {
                log.debug("Addresses resolved for domain {}: {}", domain,
                    Arrays.stream(addresses)
                        .map(InetAddress::getHostAddress)
                        .collect(Collectors.joining(", ")));
            }
            return addresses;
        } catch (UnknownHostException | SecurityException e) {
            log.debug("Cannot list IP address of domain {}", domain, e);
            return new InetAddress[0];
        }
    }

    String getIpAddress(String domain) {
        if (StringUtils.isBlank(domain)) {
            return null;
        }
        var allowedAddresses = domainToIpAddresses.get(domain, this::getInetAddresses);
        if (ArrayUtils.isEmpty(allowedAddresses)) {
            return null;
        }
        return Arrays.stream(allowedAddresses)
            .filter(Inet4Address.class::isInstance)
            .findFirst()
            .orElse(allowedAddresses[0])
            .getHostAddress();
    }

    private String extractPort(String input) {
        try {
            return String.valueOf(new URL(input).getPort());
        } catch (MalformedURLException e) {
            return null;
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
        return allowedDomain.startsWith("*.");
    }

    boolean isMatchingWildCard(String domain, String wildCard) {
        return domain.endsWith(wildCard.substring(1));
    }

}
