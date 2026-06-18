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

    private static final Pattern gatewayRoutesPattern = Pattern.compile("apiml\\.routes.*.gateway\\S*");
    private static final List<String> CORS_ENABLED_ENDPOINTS = Arrays.asList("/*/*/gateway/**", "/gateway/*/*/**", "/gateway/version");

    private final boolean gatewayCorsEnabled;
    private final List<String> defaultAllowedCorsHttpMethods;
    private final List<String> defaultAllowedOrigins;

    public boolean isCorsEnabledForService(Map<String, String> metadata) {
        String isCorsEnabledForService = metadata.get("apiml.corsEnabled");
        return Boolean.parseBoolean(isCorsEnabledForService);
    }

    public void setCorsConfiguration(Map<String, String> metadata, BiConsumer<String, CorsConfiguration> routeEntryMapper) {
        if (gatewayCorsEnabled) {
            CorsConfiguration corsConfiguration = setCorsHeadersForService(metadata);
            metadata.entrySet().stream()
                .filter(entry -> gatewayRoutesPattern.matcher(entry.getKey()).find())
                .forEach(entry ->
                    routeEntryMapper.accept(entry.getValue(), corsConfiguration));
        } else {
            log.debug("CORS is not enabled in Gateway");
        }
    }

    private CorsConfiguration setCorsHeadersForService(Map<String, String> metadata) {
        // Check if the configuration specifies allowed origins for this service
        final CorsConfiguration config = new CorsConfiguration();
        if (isCorsEnabledForService(metadata)) {
            defaultAllowedOrigins.forEach(config::addAllowedOrigin);
            String corsAllowedOriginsForService = metadata.get("apiml.corsAllowedOrigins");
            if (isNotBlank(corsAllowedOriginsForService)) {
                // Origins specified: split by comma, add to whitelist
                // apiml.corsAllowedOrigins = https://www.google.com:443,https://foo.bar:1234,*
                Arrays.stream(corsAllowedOriginsForService.split(","))
                    .forEach(config::addAllowedOrigin)
                    ;
            }
            config.setAllowCredentials(true);

            String allowedHeadersForService = metadata.get("apiml.corsAllowedHeaders");
            if (isNotBlank(allowedHeadersForService)) {
                config.setAllowedHeaders(Arrays.asList(allowedHeadersForService.split(",")));
            } else {
                config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
            }

            config.setAllowedMethods(defaultAllowedCorsHttpMethods);

            log.debug("CORS enabled for service {}: {}", metadata.get("apiml.service.title"), config);
        } else {
            config.setAllowedOrigins(defaultAllowedOrigins);
            log.debug("CORS is not enabled for service {}. Using defaults {}", metadata.get("apiml.service.title"), defaultAllowedOrigins);
        }
        return config;
    }

    public void registerDefaultCorsConfiguration(BiConsumer<String, CorsConfiguration> pathMapper) {
        final CorsConfiguration config = new CorsConfiguration();
        List<String> pathsToEnable;

        config.setAllowedOrigins(defaultAllowedOrigins);
        if (gatewayCorsEnabled) {
            config.setAllowCredentials(true);
            config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
            config.setAllowedMethods(defaultAllowedCorsHttpMethods);
            pathsToEnable = CORS_ENABLED_ENDPOINTS;
        } else {
            pathsToEnable = Collections.singletonList("/**");
        }
        pathsToEnable.forEach(path -> pathMapper.accept(path, config));
    }

}
