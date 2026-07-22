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

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.ApimlConstants.DEFAULT_ALLOWED_DOMAINS;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";
    private static final String ORG_ZOWE_APIML_COMMON_SCHEME_NOT_ALLOWED = "org.zowe.apiml.common.schemeNotAllowed";

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

    private boolean isAllowedDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            return true;
        }
        // Some services may not have correct path set, so it may be a malformed URL
        var domainToCheck = domain.endsWith("null") ? domain.substring(0, domain.lastIndexOf("null")) : domain;
        return allowedDomainsSet.stream().anyMatch(allowedDomain -> {
            try {
                return isAllowed(allowedDomain, domainToCheck);
            } catch (MalformedURLException e) {
                return false;
            }
        });
    }

    private boolean isAllowedScheme(String url) {
        if (StringUtils.isBlank(url)) {
            return true;
        }

        if (isUrl(url)) {
            String scheme = URI.create(url).getScheme();
            return !"http".equalsIgnoreCase(scheme) || isClientAttlsEnabled;
        }

        return true;
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

    private boolean validateUrl(String label, String url, InstanceInfo info) {
        if (StringUtils.isBlank(url)) {
            return true;
        }

        boolean isValid = true;

        if (!isAllowedDomain(url)) {
            // temporarly
            log.error("URL_CHECK_FAILED: Domain not allowed for '{}' -> URL: '{}'", label, url);
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, label, url, info.getInstanceId());
            isValid = false;
        }

        if (!isAllowedScheme(url)) {
            // temporarly
            log.error("URL_CHECK_FAILED: Scheme not allowed for '{}' -> URL: '{}'", label, url);
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
