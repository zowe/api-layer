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

import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.ApimlConstants.DEFAULT_ALLOWED_DOMAINS;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";
    private static final String ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED = "org.zowe.apiml.common.schemeNotAllowed";

    private final Cache<String, InetAddress[]> domainToIpAddresses = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    private final Cache<InetAddress, Boolean> ipAllowed = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname:localhost}}")
    private String allowedDomains;

    @Value("${server.attlsClient.enabled:false}")
    private boolean isClientAttlsEnabled;

    private boolean onlyWarn = false;

    @InjectApimlLogger
    private final ApimlLogger apimlLogger = ApimlLogger.empty();

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

    boolean isAllowedDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            return true;
        }
        var domainToCheck = domain.endsWith("null") ? domain.substring(0, domain.lastIndexOf("null")) : domain;
        return allowedDomainsSet.stream().anyMatch(allowedDomain -> isAllowed(allowedDomain, domainToCheck));
    }

    private InetAddress[] getInetAddresses(String domain) {
        try {
            return InetAddress.getAllByName(domain);
        } catch (UnknownHostException | SecurityException e) {
            log.debug("Cannot list IP address of domain {}", domain, e);
            return new InetAddress[0];
        }
    }

    private boolean isAllowedIpAddress(String allowed, InetAddress address, String hostname) {
        try {
            // check if the allowed domain is not written in IP format
            if (InetAddresses.forString(allowed).equals(address)) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            log.trace("Domain {} is not a IP address", allowed);
        }

        // if allowed domain is wildcard replace with hostname if matching, otherwise it cannot be evaluated
        if (isWildCard(allowed)) {
            if (!StringUtils.isBlank(hostname) && isMatchingWildCard(hostname, allowed)) {
                allowed = hostname;
            } else {
                return false;
            }
        }

        // obtain list of domain's IP address and check if any is matching
        var allowedAddresses = domainToIpAddresses.get(allowed, this::getInetAddresses);
        return Arrays.stream(allowedAddresses).anyMatch(address::equals);
    }

    boolean isAllowedIpAddress(String label, String ipAddress, InstanceInfo info) {
        if (StringUtils.isBlank(ipAddress)) {
            return true;
        }

        // check if the domain list contains the same literal directly
        if (isAllowedDomain(ipAddress)) {
            return true;
        }

        var address = InetAddresses.forString(ipAddress);
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            // local address (ie. loopback 127.0.0.1) is allowed as default
            return true;
        }

        // check cache and if entry misses verify ip against all allowed domains
        var hostname = info.getHostName();
        var allowed = ipAllowed.get(address, ip ->
            allowedDomainsSet.stream().anyMatch(allowedDomain ->
                isAllowedIpAddress(allowedDomain, ip, hostname)
            )
        );

        if (!allowed) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, label, info.getIPAddr(), info.getInstanceId());
        }

        return allowed;
    }

    private String extractDomain(String url) {
        try {
            return new URL(url).getHost().toLowerCase();
        } catch (MalformedURLException e) {
            log.debug("'{}' is not a valid URL", url);
            return url;
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

    private boolean isAllowed(String allowedDomain, String domain) {
        log.debug("checking URL {} against domain {}", domain, allowedDomain);
        allowedDomain = allowedDomain.toLowerCase();
        domain = domain.toLowerCase();
        domain = extractDomain(domain);
        if (domain.equals(allowedDomain)) {
            return true;
        }
        if (isWildCard(allowedDomain)) {
            return isMatchingWildCard(domain, allowedDomain);
        }

        return false;
    }

    private boolean validateUrl(String label, String url, InstanceInfo info) {
        if (StringUtils.isBlank(url)) {
            return true;
        }

        boolean isValid = true;

        if (!isAllowedDomain(url)) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, label, url, info.getInstanceId());
            isValid = false;
        }

        if (!isAllowedScheme(url)) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED, label, url, info.getInstanceId());
            isValid = false;
        }

        if (isValid && log.isTraceEnabled()) {
            log.trace("URL {} is allowed for {}", url, label);
        }

        return isValid;
    }

    private boolean verifyMetadataEntry(String key, String value, InstanceInfo info) {
        var metadataKeysToVerify = List.of(
            "swaggerUrl",
            "graphqlUrl",
            "documentationUrl",
            "externalUrl");

        if (metadataKeysToVerify.stream().anyMatch(metadataKey -> key.startsWith("apiml.") && key.endsWith(metadataKey))) {
            return validateUrl(key, value, info);
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
                if (!validateUrl("API ML CORS Allowed Origin", url, info)) {
                    result.set(false);
                }
            });
        }

        return result.get();
    }

    public void verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var result = new AtomicBoolean(true);
        if (!isAllowedIpAddress("IP Address", info.getIPAddr(), info)) {
            result.set(false);
        }
        if (!validateUrl("Instance Hostname", info.getHostName(), info)) {
            result.set(false);
        }
        if (!validateUrl("Home Page URL", info.getHomePageUrl(), info)) {
            result.set(false);
        }
        if (!validateUrl("HealthCheck URL", info.getHealthCheckUrl(), info)) {
            result.set(false);
        }
        if (!validateUrl("Status Page URL", info.getStatusPageUrl(), info)) {
            result.set(false);
        }
        if (!validateUrl("Secure Health Check URL", info.getSecureHealthCheckUrl(), info)) {
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

}
