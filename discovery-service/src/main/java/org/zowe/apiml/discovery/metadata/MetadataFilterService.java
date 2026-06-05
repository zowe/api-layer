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
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname}}")
    private String allowedDomains;

    private boolean onlyWarn = false;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    private List<String> allowedDomainsList;

    @Override
    public void afterPropertiesSet() throws Exception {
        allowedDomainsList = Arrays.stream(allowedDomains.split(",")).map(String::trim).toList();
        onlyWarn = Optional.ofNullable(System.getenv("ZWE_ONLY_WARN_ON_URL_NOT_ALLOWED")).map(Boolean::parseBoolean).orElse(false);
    }

    private boolean isAllowedDomain(String domain) {
        if (StringUtils.isBlank(domain)) {
            return true;
        }
        return allowedDomainsList.stream().anyMatch(allowedDomain -> {
            try {
                return isAllowed(allowedDomain, domain);
            } catch (MalformedURLException e) {
                return false;
            }
        });
    }

    private boolean isAllowed(String allowedDomain, String domain) throws MalformedURLException {
        log.debug("checking URL {} against domain {}", domain, allowedDomain);
        if (isUrl(domain)) {
            domain = new URL(domain).getHost();
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
