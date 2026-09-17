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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
class IPAddressUtil {

    private static final Cache<String, InetAddress[]> DOMAIN_TO_IP_ADDRESSES = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    private IPAddressUtil() {}

    private static boolean isIPV6(String input) {
        try {
            var str = StringUtils.substringBetween(input, "[", "]");
            new IpAddressMatcher(str == null ? input : str);
            return input.contains(":");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isIPV4(String input) {
        try {
            new IpAddressMatcher(input);
            return !input.contains(":");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean isIPV6Single(String input) {
        try {
            var str = StringUtils.substringBetween(input, "[", "]");
            new IpAddressMatcher(str == null ? input : str);
            return input.contains(":") && !input.contains("/");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean isIPV6CIDR(String input) {
        return isIPV6(input) && input.contains("/");
    }

    static boolean isIPV4CIDR(String input) {
        return isIPV4(input) && input.contains("/");
    }

    static InetAddress[] getInetAddresses(String domain) {
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

    static String getIpAddress(String domain) {
        if (StringUtils.isBlank(domain)) {
            return null;
        }
        var allowedAddresses = DOMAIN_TO_IP_ADDRESSES.get(domain, IPAddressUtil::getInetAddresses);
        if (ArrayUtils.isEmpty(allowedAddresses)) {
            return null;
        }
        return Arrays.stream(allowedAddresses)
            .filter(Inet4Address.class::isInstance)
            .findFirst()
            .orElse(allowedAddresses[0])
            .getHostAddress();
    }

    static InetAddress[] getIPAddresses(String allowedDomain) {
        return DOMAIN_TO_IP_ADDRESSES.get(allowedDomain, IPAddressUtil::getInetAddresses);
    }

    static String getPortIPV6(String ipV6String) {
        var remaining = StringUtils.substringAfter(ipV6String, "]");
        var port = StringUtils.substringAfter(remaining, ":");

        if (StringUtils.isNotBlank(port)) {
            return port;
        }
        return null;
    }

    static String getHostIPV6(String ipV6String) {
        var ipAddress = StringUtils.substringBetween(ipV6String, "[", "]");
        return ipAddress == null ? ipV6String : ipAddress;
    }

}
