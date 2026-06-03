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
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    private static final String ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED = "org.zowe.apiml.common.urlNotAllowed";

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname}}")
    private String allowedDomains;

    @InjectApimlLogger
    private ApimlLogger apimlLogger = ApimlLogger.empty();

    private List<String> allowedDomainsList;

    @Override
    public void afterPropertiesSet() throws Exception {
        allowedDomainsList = Arrays.stream(allowedDomains.split(",")).map(String::trim).toList();
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

    private boolean isAllowed(String allowedDomain, String value) throws MalformedURLException {
        log.debug("checking URL {} against domain {}", value, allowedDomain);
        if (isUrl(value)) {
            value = new URL(value).getHost();
        }
        if (value.equals(allowedDomain)) {
            return true;
        }
        if (allowedDomain.startsWith("*.")) {
            return value.endsWith(allowedDomain.substring(2));
        }

        return false;
    }

    private boolean verifyMetadataEntry(String key, String value, InstanceInfo info) {
        var metadataToVerify = List.of(
            "gatewayUrl",
            "gateway-url",
            "serviceUrl",
            "service-url",
            "swaggerUrl",
            "graphqlUrl",
            "documentationUrl");

        if (metadataToVerify.stream().anyMatch(key::endsWith) && isUrl(value)) {
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
        urls.forEach(url -> {
            if (url.equals("*")) {
                apimlLogger.log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
                return;
            }
            if (!isAllowedDomain(url)) {
                apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "API ML CORS Allowed Origin", url, info.getInstanceId());
                result.set(false);
            }
        });
        return result.get();
    }

    public void verifyAllowedDomains(InstanceInfo info) throws MetadataValidationException {
        var result = true;
        if (!isAllowedDomain(info.getHomePageUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "Home Page URL", info.getHomePageUrl(), info.getInstanceId());
            result = false;
        }
        if (!isAllowedDomain(info.getHealthCheckUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "HealthCheck URL", info.getHealthCheckUrl(), info.getInstanceId());
            result = false;
        }
        if (!isAllowedDomain(info.getStatusPageUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "Status Page URL", info.getStatusPageUrl(), info.getInstanceId());
            result = false;
        }
        if (!isAllowedDomain(info.getSecureHealthCheckUrl())) {
            apimlLogger.log(ORG_ZOWE_APIML_COMMON_URL_NOT_ALLOWED, "Secure Health Check URL", info.getSecureHealthCheckUrl(), info.getInstanceId());
            result = false;
        }

        if (info.getMetadata().containsKey("apiml.corsAllowedOrigins")) {
            var corsVerificationResult = verifyCorsAllowedOrigins(info.getMetadata().get("apiml.corsAllowedOrigins"), info);
            if (!corsVerificationResult) {
                result = false;
            }
        }

        info.getMetadata().forEach((key, value) -> verifyMetadataEntry(key, value, info));

        if (!result) {
            throw new MetadataValidationException("URLs not allowed found for instance " + info.getInstanceId());
        }

    }

    private boolean isUrl(String value) {
        try {
            new URL(value);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }

    }

}
