/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.metadata;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.net.InetAddresses;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.ApimlConstants.DEFAULT_ALLOWED_DOMAINS;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";

    private static final Cache<String, InetAddress[]> DOMAIN_TO_IP_ADDRESSES = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    private static final Cache<InetAddress, Boolean> IP_ALLOWED = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname:localhost}}")
    private String allowedDomains;

    private boolean onlyWarn = false;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    private Set<String> allowedDomainsSet;

    @Override
    public void afterPropertiesSet() {
        allowedDomainsSet = Stream.concat(Arrays.stream(allowedDomains.split(",")).map(String::trim), Arrays.stream(DEFAULT_ALLOWED_DOMAINS)).map(String::toLowerCase).collect(Collectors.toSet());
        onlyWarn = Optional.ofNullable(System.getenv("ZWE_ONLY_WARN_ON_URL_NOT_ALLOWED")).map(Boolean::parseBoolean).orElse(false);

        log.info("Allowed domains in Discovery Service: {}", allowedDomains);

        if (onlyWarn) {
            log.info("Only warning on URL not allowed is enabled");
        }

    }

    private boolean isAllowedDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            return true;
        }
        return allowedDomainsSet.stream().anyMatch(allowedDomain -> {
            try {
                return isAllowed(allowedDomain, domain);
            } catch (MalformedURLException e) {
                return false;
            }
        });
    }

    private InetAddress[] getInetAddresses(String domain) {
        try {
            return InetAddress.getAllByName(domain);
        } catch (UnknownHostException | SecurityException e) {
            log.debug("Cannot list IP address of domain {}", domain, e);
            return new InetAddress[0];
        }
    }

    private boolean isAllowedIpAddress(String allowed, InetAddress address) {
        try {
            // check if the allowed domain is not written in IP format
            if (InetAddresses.forString(allowed).equals(address)) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            log.trace("Domain {} is not a IP address", allowed);
        }

        // obtain list of domain's IP address and check if any is matching
        var allowedAddresses = DOMAIN_TO_IP_ADDRESSES.get(allowed, this::getInetAddresses);
        return Arrays.stream(allowedAddresses).anyMatch(address::equals);
    }

    boolean isAllowedIpAddress(String ipAddress) {
        if (StringUtils.isBlank(ipAddress)) {
            return true;
        }

        // check if the domain list contains the same literal directly
        if (isAllowedDomain(ipAddress)) {
            return true;
        }

        InetAddress address = InetAddresses.forString(ipAddress);
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            // local address (ie. loopback 127.0.0.1) is allowed as default
            return true;
        }

        // check cache and if entry misses verify ip against all allowed domains
        return IP_ALLOWED.get(address, ip ->
            allowedDomainsSet.stream().anyMatch(allowedDomain ->
                isAllowedIpAddress(allowedDomain, ip)
            )
        );
    }

    private boolean isAllowed(String allowedDomain, String domain) throws MalformedURLException {
        log.debug("checking URL {} against domain {}", domain, allowedDomain);
        allowedDomain = allowedDomain.toLowerCase();
        domain = domain.toLowerCase();
        if (isUrl(domain)) {
            domain = new URL(domain).getHost().toLowerCase();
        }
        if (domain.equals(allowedDomain)) {
            return true;
        }
        if (allowedDomain.startsWith("*.")) {
            return domain.endsWith(allowedDomain.substring(1));
        }

        return false;
    }

    private boolean verifyMetadataEntry(String key, String value, InstanceInfo info) {
        var metadataKeysToVerify = List.of(
            "swaggerUrl",
            "graphqlUrl",
            "documentationUrl",
            "externalUrl");

        if (metadataKeysToVerify.stream().anyMatch(metadataKey -> key.startsWith("apiml.") && key.endsWith(metadataKey))) {
            if (!isAllowedDomain(value)) {
                apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, key, value, info.getInstanceId());
                return false;
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("URL {} is allowed", value);
                }
            }
        }
        return true;

    }

    private boolean verifyCorsAllowedOrigins(String allowedOrigins, InstanceInfo info) {
        var urls = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        var result = new AtomicBoolean(true);

        if ("*".equals(allowedOrigins)) {
            apimlLogger.log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
        } else {
            urls.forEach(url -> {
                if (!isAllowedDomain(url)) {
                    apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "API ML CORS Allowed Origin", url, info.getInstanceId());
                    result.set(false);
                }
            });
        }

        return result.get();
    }

    public void verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var result = new AtomicBoolean(true);
        if (!isAllowedIpAddress(info.getIPAddr())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "IP Address", info.getIPAddr(), info.getInstanceId());
            result.set(false);
        }
        if (!isAllowedDomain(info.getHomePageUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "Home Page URL", info.getHomePageUrl(), info.getInstanceId());
            result.set(false);
        }
        if (!isAllowedDomain(info.getHealthCheckUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "HealthCheck URL", info.getHealthCheckUrl(), info.getInstanceId());
            result.set(false);
        }
        if (!isAllowedDomain(info.getStatusPageUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "Status Page URL", info.getStatusPageUrl(), info.getInstanceId());
            result.set(false);
        }
        if (!isAllowedDomain(info.getSecureHealthCheckUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "Secure Health Check URL", info.getSecureHealthCheckUrl(), info.getInstanceId());
            result.set(false);
        }

        if (info.getMetadata().containsKey("apiml.corsAllowedOrigins")) {
            var corsVerificationResult = verifyCorsAllowedOrigins(info.getMetadata().get("apiml.corsAllowedOrigins"), info);
            if (!corsVerificationResult) {
                result.set(false);
            }
        }

        info.getMetadata().forEach((key, value) -> {
            var metadataVerificationResult = verifyMetadataEntry(key, value, info);
            if (!metadataVerificationResult) {
                result.set(false);
            }
        });

        if (!result.get() && !onlyWarn) {
            throw new MetadataValidationException("URLs not allowed found for instance " + info.getInstanceId());
        }

    }

    private boolean isUrl(String value) {
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            log.debug("'{}' is not a valid URL", value);
            return false;
        }

    }

}
