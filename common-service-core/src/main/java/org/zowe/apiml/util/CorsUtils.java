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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

@Slf4j
public class CorsUtils {

    private final List<String> allowedCorsHttpMethods;
    private final boolean corsEnabled;
    private static final Pattern gatewayRoutesPattern = Pattern.compile("apiml\\.routes\\.[^.]*\\.gateway\\S*");
    private final List<String> corsAllowedEndpoints;

    public CorsUtils(boolean corsEnabled, List<String> corsAllowedMethods, @NonNull List<String> allowedEndpoints) {
        this.corsEnabled = corsEnabled;
        this.allowedCorsHttpMethods = corsAllowedMethods;
        this.corsAllowedEndpoints = allowedEndpoints;
    }

    public boolean isCorsEnabledForService(Map<String, String> metadata) {
        String isCorsEnabledForService = metadata.get("apiml.corsEnabled");
        return Boolean.parseBoolean(isCorsEnabledForService);
    }

    public void setCorsConfiguration(String serviceId, Map<String, String> metadata, TriConsumer<String, String, CorsConfiguration> entryMapper) {
        if (corsEnabled) {
            log.error("CORS is enabled");
            CorsConfiguration corsConfiguration = setAllowedOriginsForService(metadata);
            metadata.entrySet().stream()
                .filter(entry -> gatewayRoutesPattern.matcher(entry.getKey()).find())
                .forEach(entry ->
                    entryMapper.accept(entry.getValue(), serviceId, corsConfiguration));
        }
    }

    private CorsConfiguration setAllowedOriginsForService(Map<String, String> metadata) {
        // Check if the configuration specifies allowed origins for this service
        final CorsConfiguration config = new CorsConfiguration();
        if (isCorsEnabledForService(metadata)) {
            String corsAllowedOriginsForService = metadata.get("apiml.corsAllowedOrigins");
            if (corsAllowedOriginsForService == null || corsAllowedOriginsForService.isEmpty()) {
                // Origins not specified: allow everything
                config.addAllowedOriginPattern(CorsConfiguration.ALL);
            } else {
                // Origins specified: split by comma, add to whitelist
                Arrays.stream(corsAllowedOriginsForService.split(","))
                    .forEach(config::addAllowedOrigin);
            }
            config.setAllowCredentials(true);
            config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
            config.setAllowedMethods(allowedCorsHttpMethods);
        }
        return config;
    }

    public void registerDefaultCorsConfiguration(BiConsumer<String, CorsConfiguration> pathMapper) {
        final CorsConfiguration config = new CorsConfiguration();
        List<String> pathsToEnable;

        log.error("CORS enabled: {} REGISTER DEFAULT CORS CONFIGURATION {}", corsEnabled, Arrays.toString(corsAllowedEndpoints.toArray()));

        if (corsEnabled) {
            config.setAllowCredentials(true);
            config.addAllowedOriginPattern(CorsConfiguration.ALL); //NOSONAR this is a replication of existing code
            config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
            config.setAllowedMethods(allowedCorsHttpMethods);
            pathsToEnable = corsAllowedEndpoints;
        } else {
            pathsToEnable = Collections.singletonList("/**");
        }
        pathsToEnable.forEach(path -> pathMapper.accept(path, config));
    }
}
