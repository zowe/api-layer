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

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.stereotype.Service;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.eureka.DomainAllowListMetadataException;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.ApimlConstants.DEFAULT_ALLOWED_DOMAINS;

@Service
@Slf4j
public class MetadataFilterService implements InitializingBean {

    private static final String INSTANCE_HOSTNAME = "Instance Hostname";
    private static final String ZWE_ONLY_WARN_ON_URL_NOT_ALLOWED = "ZWE_ONLY_WARN_ON_URL_NOT_ALLOWED";
    private static final String ZWE_DISABLE_PORT_VALIDATION = "ZWE_DISABLE_PORT_VALIDATION";

    private static final String HTTPS = "https://";
    private static final String HTTP = "http://";

    // map k: metadata key, v: whether to validate port or not
    private static final Map<String, Boolean> METADATA_URL_KEYS_TO_VERIFY = Map.of(
        "swaggerUrl", true,
        "graphqlUrl", true,
        "documentationUrl", false,
        "externalUrl", true
    );

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname:localhost}}")
    private String allowedDomains;

    @Value("${server.attlsClient.enabled:false}")
    private boolean isClientAttlsEnabled;

    private boolean onlyWarn = false;
    private boolean disablePortValidation = false;

    @InjectApimlLogger
    private final ApimlLogger apimlLogger = ApimlLogger.empty();

    private Set<String> allowedDomainsSet;

    @Override
    public void afterPropertiesSet() {
        allowedDomainsSet = sanitizeAllowedDomains();
        onlyWarn = Optional.ofNullable(System.getenv(ZWE_ONLY_WARN_ON_URL_NOT_ALLOWED)).map(Boolean::parseBoolean).orElse(false);
        disablePortValidation = Optional.ofNullable(System.getenv(ZWE_DISABLE_PORT_VALIDATION)).map(Boolean::parseBoolean).orElse(false);

        log.info("Allowed domains: {}", allowedDomains);

        if (onlyWarn) {
            log.info("Only warning on URL not allowed is enabled");
        }
        if (disablePortValidation) {
            log.info("Port validation in domain allow list is disabled");
        }
    }

    private Set<String> sanitizeAllowedDomains() {
        var set = Stream.concat(Arrays.stream(allowedDomains.split(",")).map(String::trim), Arrays.stream(DEFAULT_ALLOWED_DOMAINS)).map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> resultSet = new HashSet<>();
        set.forEach(domain -> {
            if (Strings.CI.startsWithAny(domain, HTTP, HTTPS)) {
                var replacement = domain.replace(HTTP, "").replace(HTTPS, "");
                log.warn("Allowed domains list must not include schemes, entry {} replaced with {}", domain, replacement);
                resultSet.add(replacement);
            } else if (Strings.CI.endsWith(domain, ":")) {
                log.warn("Entry {} is not properly formed. It will be ignored", domain);
            } else {
                resultSet.add(domain);
            }
        });
        return resultSet;
    }

    private boolean validateMetadataEntry(String key, String value, MetadataValidator validator) {
        if (!key.startsWith(EurekaMetadataDefinition.API_INFO)) {
            return true;
        }
        var segments = key.split("\\.", -1);   // -1 keeps the empty trailing segment
        if (segments.length < 4) {
            return true;
        }

        var segmentKey = segments[3];

        return METADATA_URL_KEYS_TO_VERIFY.entrySet().stream()
            .filter(e -> e.getKey().equals(segmentKey))
            .findFirst()
            .map(entry -> validator.validateEntry(key, value, entry.getValue()))
            .orElse(true);
    }

    private boolean verifyCorsAllowedOrigins(String allowedOrigins, MetadataValidator validator) {
        var urls = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        var result = new AtomicBoolean(true);

        if ("*".equals(allowedOrigins)) {
            apimlLogger.log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
        } else {
            urls.forEach(url -> {
                if (!validator.validateEntry("API ML CORS Allowed Origin", url, false)) {
                    result.set(false);
                }
            });
        }

        return result.get();
    }

    /**
     *
     * @param instance InstanceInfo to validate
     * @return InstanceInfo, may be updated
     * @throws MetadataValidationException If one or more validations failed
     */
    @SuppressWarnings("java:S3776")
    public EurekaServiceInstance verifyAllowedDomains(EurekaServiceInstance instance) throws MetadataValidationException {
        var result = new AtomicBoolean(true);
        var resultInstance = instance;

        var validator = MetadataValidator.builder()
            .allowedDomainsSet(allowedDomainsSet)
            .apimlLogger(apimlLogger)
            .instanceInfo(instance.getInstanceInfo())
            .isClientAttlsEnabled(isClientAttlsEnabled)
            .disablePortValidation(disablePortValidation)
            .build();

        if (!validator.validateEntry("IP Address", instance.getInstanceInfo().getIPAddr(), false)) {
            log.debug("IP address {} is not allowed. It is removed from the registration data.", instance.getInstanceInfo().getIPAddr());
            // this is updating the same instance even it looks like creating a new instance of InstanceInfo
            resultInstance = new EurekaServiceInstance(new InstanceInfo.Builder(instance.getInstanceInfo()).setIPAddr(IPAddressUtil.getIpAddress(instance.getHost())).build());
        }
        if (disablePortValidation) {
            if (!validator.validateEntry(INSTANCE_HOSTNAME, instance.getHost(), false)) {
                result.set(false);
            }
        } else if (!validator.validateEntry(INSTANCE_HOSTNAME, instance.getHost() + ":" + instance.getPort(), true)) {
            result.set(false);
        }

        if (!validator.validateEntry("Home Page URL", instance.getInstanceInfo().getHomePageUrl(), true)) {
            result.set(false);
        }
        if (!validator.validateEntry("HealthCheck URL", instance.getInstanceInfo().getHealthCheckUrl(), true)) {
            result.set(false);
        }
        if (!validator.validateEntry("Status Page URL", instance.getInstanceInfo().getStatusPageUrl(), true)) {
            result.set(false);
        }
        if (!validator.validateEntry("Secure Health Check URL", instance.getInstanceInfo().getSecureHealthCheckUrl(), true)) {
            result.set(false);
        }

        if (instance.getMetadata().containsKey("apiml.corsAllowedOrigins")) {
            var corsVerificationResult = verifyCorsAllowedOrigins(instance.getMetadata().get("apiml.corsAllowedOrigins"), validator);
            if (!corsVerificationResult) {
                result.set(false);
            }
        }

        for (Map.Entry<String, String> entry : instance.getMetadata().entrySet()) {
            var metadataVerificationResult = validateMetadataEntry(entry.getKey(), entry.getValue(), validator);
            if (!metadataVerificationResult) {
                result.set(false);
            }
        }

        if (!result.get() && !onlyWarn) {
            throw new DomainAllowListMetadataException("URLs not allowed found for instance " + instance.getInstanceId());
        }

        return resultInstance;
    }

}
