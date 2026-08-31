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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final List<String> METADATA_KEYS_TO_VERIFY = List.of(
        "swaggerUrl",
        "graphqlUrl",
        "documentationUrl",
        "externalUrl"
    );

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

    private boolean validateMetadataEntry(String key, String value, MetadataValidator validator) {
        if (!key.startsWith("apiml.")) return true;
        var segments = key.split("\\.", -1);   // -1 keeps the empty trailing segment
        boolean sensitive = Arrays.stream(segments).anyMatch(METADATA_KEYS_TO_VERIFY::contains);
        return !sensitive || validator.validateEntry(key, value);
    }

    private boolean verifyCorsAllowedOrigins(String allowedOrigins, MetadataValidator validator) {
        var urls = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        var result = new AtomicBoolean(true);

        if ("*".equals(allowedOrigins)) {
            apimlLogger.log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
        } else {
            urls.forEach(url -> {
                if (!validator.validateEntry("API ML CORS Allowed Origin", url)) {
                    result.set(false);
                }
            });
        }

        return result.get();
    }

    /**
     *
     * @param instanceInfo InstanceInfo to validate
     * @return InstanceInfo, may be updated
     * @throws MetadataValidationException If one or more validations failed
     */
    public InstanceInfo verifyAllowedDomains(InstanceInfo instanceInfo) throws MetadataValidationException {
        var result = new AtomicBoolean(true);
        var validator = MetadataValidator.builder()
            .allowedDomainsSet(allowedDomainsSet)
            .apimlLogger(apimlLogger)
            .instanceInfo(instanceInfo)
            .isClientAttlsEnabled(isClientAttlsEnabled)
            .build();

        if (!validator.validateEntry("IP Address", instanceInfo.getIPAddr())) {
            log.debug("IP address {} is not allowed. It is removed from the registration data.", instanceInfo.getIPAddr());
            // this is updating the same instance even it looks like creating a new instance of InstanceInfo
            instanceInfo = new InstanceInfo.Builder(instanceInfo).setIPAddr(getIpAddress(instanceInfo.getHostName())).build();
        }
        if (!validator.validateEntry("Instance Hostname", instanceInfo.getHostName())) {
            result.set(false);
        }
        if (!validator.validateEntry("Home Page URL", instanceInfo.getHomePageUrl())) {
            result.set(false);
        }
        if (!validator.validateEntry("HealthCheck URL", instanceInfo.getHealthCheckUrl())) {
            result.set(false);
        }
        if (!validator.validateEntry("Status Page URL", instanceInfo.getStatusPageUrl())) {
            result.set(false);
        }
        if (!validator.validateEntry("Secure Health Check URL", instanceInfo.getSecureHealthCheckUrl())) {
            result.set(false);
        }

        if (instanceInfo.getMetadata().containsKey("apiml.corsAllowedOrigins")) {
            var corsVerificationResult = verifyCorsAllowedOrigins(instanceInfo.getMetadata().get("apiml.corsAllowedOrigins"), validator);
            if (!corsVerificationResult) {
                result.set(false);
            }
        }

        for (Map.Entry<String, String> entry : instanceInfo.getMetadata().entrySet()) {
            var metadataVerificationResult = validateMetadataEntry(entry.getKey(), entry.getValue(), validator);
            if (!metadataVerificationResult) {
                result.set(false);
            }
        }

        if (!result.get() && !onlyWarn) {
            throw new DomainAllowListMetadataException("URLs not allowed found for instance " + instanceInfo.getInstanceId());
        }

        return instanceInfo;
    }

}
