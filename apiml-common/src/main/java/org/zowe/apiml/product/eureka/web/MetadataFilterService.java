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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.eureka.DomainAllowListMetadataException;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    private static final String ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED = "org.zowe.apiml.common.schemeNotAllowed";
    private static final List<String> METADATA_KEYS_TO_VERIFY = List.of(
        "swaggerUrl",
        "graphqlUrl",
        "documentationUrl",
        "externalUrl"
    );

    private final Cache<String, InetAddress[]> domainToIpAddresses = Caffeine.newBuilder()
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

    boolean isAllowedDomain(String input) {
        if (StringUtils.isBlank(input)) {
            return true;
        }
        var inputToCheck = input.endsWith("null") ? input.substring(0, input.lastIndexOf("null")) : input;
        return allowedDomainsSet.stream().anyMatch(allowedDomain -> isAllowed(allowedDomain, inputToCheck));
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

    private boolean isAllowed(String allowedDomain, String input) {
        log.debug("checking domain {} against allowed domain {}", input, allowedDomain);
        allowedDomain = allowedDomain.toLowerCase();
        input = input.toLowerCase();
        input = extractDomain(input);
        if (input == null) {
            return false;
        }
        if (input.equals(allowedDomain)) {
            return true;
        }
        if (isWildCard(allowedDomain)) {
            return isMatchingWildCard(input, allowedDomain);
        }
        return isAllowedIpAddress(input, allowedDomain);
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

    private boolean validateEntry(String label, String input, String instanceId) {
        if (StringUtils.isBlank(input)) {
            return true;
        }

        boolean isValid = true;

        if (!isAllowedDomain(input)) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, label, input, instanceId);
            isValid = false;
        }

        if (!isAllowedScheme(input)) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED, label, input, instanceId);
            isValid = false;
        }

        if (isValid && log.isTraceEnabled()) {
            log.trace("URL {} is allowed for {}", input, label);
        }

        return isValid;
    }

    private boolean validateMetadataEntry(String key, String value, String instanceId) {
        if (!key.startsWith("apiml.")) return true;
        var segments = key.split("\\.", -1);   // -1 keeps the empty trailing segment
        boolean sensitive = Arrays.stream(segments).anyMatch(METADATA_KEYS_TO_VERIFY::contains);
        return !sensitive || validateEntry(key, value, instanceId);
    }

    private boolean verifyCorsAllowedOrigins(String allowedOrigins, String instanceId) {
        var urls = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        var result = new AtomicBoolean(true);

        if ("*".equals(allowedOrigins)) {
            apimlLogger.log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
        } else {
            urls.forEach(url -> {
                if (!validateEntry("API ML CORS Allowed Origin", url, instanceId)) {
                    result.set(false);
                }
            });
        }

        return result.get();
    }

    public InstanceInfo verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var result = new AtomicBoolean(true);
        var instanceId = info.getInstanceId();

        if (!validateEntry("IP Address", info.getIPAddr(), instanceId)) {
            log.debug("IP address {} is not allowed. It is removed from the registration data.", info.getIPAddr());
            // this is updating the same instance even it looks like creating a new instance of InstanceInfo
            info = new InstanceInfo.Builder(info).setIPAddr(getIpAddress(info.getHostName())).build();
        }
        if (!validateEntry("Instance Hostname", info.getHostName(), instanceId)) {
            result.set(false);
        }
        if (!validateEntry("Home Page URL", info.getHomePageUrl(), instanceId)) {
            result.set(false);
        }
        if (!validateEntry("HealthCheck URL", info.getHealthCheckUrl(), instanceId)) {
            result.set(false);
        }
        if (!validateEntry("Status Page URL", info.getStatusPageUrl(), instanceId)) {
            result.set(false);
        }
        if (!validateEntry("Secure Health Check URL", info.getSecureHealthCheckUrl(), instanceId)) {
            result.set(false);
        }

        if (info.getMetadata().containsKey("apiml.corsAllowedOrigins")) {
            var corsVerificationResult = verifyCorsAllowedOrigins(info.getMetadata().get("apiml.corsAllowedOrigins"), instanceId);
            if (!corsVerificationResult) {
                result.set(false);
            }
        }

        info.getMetadata().forEach((key, value) -> {
            var metadataVerificationResult = validateMetadataEntry(key, value, instanceId);
            if (!metadataVerificationResult) {
                result.set(false);
            }
        });

        if (!result.get() && !onlyWarn) {
            throw new DomainAllowListMetadataException("URLs not allowed found for instance " + instanceId);
        }

        return info;
    }

}
