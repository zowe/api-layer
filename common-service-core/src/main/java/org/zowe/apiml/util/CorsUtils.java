/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Builder
public class CorsUtils {

    private static final Pattern gatewayRoutesPattern = Pattern.compile("apiml\\.routes\\.[^.]*\\.gateway\\S*");

    private final List<String> defaultAllowedCorsHttpMethods;
    private final boolean gatewayCorsEnabled;
    private final List<String> corsAllowedEndpoints;
    private final List<String> defaultAllowedCorsOrigins;
    private final List<String> defaultAllowedCorsHeaders;
    private final boolean defaultAllowCredentials;

    public CorsUtils(
            List<String> corsAllowedMethods,
            boolean corsEnabled,
            @NonNull List<String> allowedEndpoints,
            @NonNull List<String> defaultAllowedCorsOrigins,
            @NonNull List<String> defaultAllowedCorsHeaders,
            boolean defaultAllowCredentials) {
        this.defaultAllowedCorsHttpMethods = corsAllowedMethods;
        this.gatewayCorsEnabled = corsEnabled;
        this.corsAllowedEndpoints = allowedEndpoints;
        this.defaultAllowedCorsOrigins = defaultAllowedCorsOrigins;
        this.defaultAllowedCorsHeaders = defaultAllowedCorsHeaders;
        this.defaultAllowCredentials = defaultAllowCredentials;
    }

    public boolean isCorsEnabledForService(Map<String, String> metadata) {
        var isCorsEnabledForService = metadata.get("apiml.corsEnabled");
        return Boolean.parseBoolean(isCorsEnabledForService);
    }

    public void setCorsConfiguration(String serviceId, Map<String, String> metadata, BiConsumer<String, CorsConfiguration> entryMapper) {
        if (gatewayCorsEnabled) {
            var corsConfiguration = createCorsConfigurationForService(serviceId, metadata);
            metadata.entrySet().stream()
                .filter(entry -> gatewayRoutesPattern.matcher(entry.getKey()).find())
                .forEach(entry ->
                    entryMapper.accept(entry.getValue(), corsConfiguration));
        } else {
            log.debug("CORS is not enabled in Gateway");
        }
    }

    private CorsConfiguration createCorsConfigurationForService(String serviceId, Map<String, String> metadata) {
        // Check if the configuration specifies allowed origins for this service
        var config = new CorsConfiguration();
        if (isCorsEnabledForService(metadata)) {

            defaultAllowedCorsOrigins.forEach(config::addAllowedOrigin);

            var corsAllowedOriginsForService = metadata.get("apiml.corsAllowedOrigins");
            var allowedHeadersForService = metadata.get("apiml.corsAllowedHeaders");
            var allowedCredentialsForService = metadata.get("");
            var allowedMethodsForService = metadata.get("");

            if (isNotBlank(corsAllowedOriginsForService)) {
                // Origins specified: split by comma, add to whitelist
                log.debug("For service {}, set [{}] as allowed origins", serviceId, Arrays.toString(corsAllowedOriginsForService.split(",")));
                Arrays.stream(corsAllowedOriginsForService.split(","))
                    .forEach(config::addAllowedOrigin);
            }

            if (isNotBlank(allowedCredentialsForService)) {
                config.setAllowCredentials(Boolean.parseBoolean(allowedCredentialsForService));
            } else {
                config.setAllowCredentials(defaultAllowCredentials);
            }

            if (isNotBlank(allowedMethodsForService)) {
                config.setAllowedMethods(Arrays.asList(allowedMethodsForService.split(",")));
            } else {
                config.setAllowedMethods(defaultAllowedCorsHttpMethods);
            }

            if (isNotBlank(allowedHeadersForService)) {
                config.setAllowedHeaders(Arrays.asList(allowedHeadersForService.split(",")));
            } else {
                config.setAllowedHeaders(defaultAllowedCorsHeaders);
            }
        } else {
            config.setAllowedOrigins(defaultAllowedCorsOrigins);
            log.debug("CORS is not enabled for service {}, using defaults", serviceId);
        }
        return config;
    }

    public void registerDefaultCorsConfiguration(BiConsumer<String, CorsConfiguration> pathMapper) {
        var config = new CorsConfiguration();
        List<String> pathsToEnable;

        config.setAllowedOrigins(defaultAllowedCorsOrigins);
        if (gatewayCorsEnabled) {
            config.setAllowCredentials(true);
            config.setAllowedHeaders(defaultAllowedCorsHeaders);
            config.setAllowedMethods(defaultAllowedCorsHttpMethods);
            pathsToEnable = corsAllowedEndpoints;
        } else {
            pathsToEnable = Collections.singletonList("/**");
        }
        pathsToEnable.forEach(path -> pathMapper.accept(path, config));
    }

}
