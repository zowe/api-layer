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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class CorsUtils {
    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;

    private final CorsConfigurationSource corsConfigurationSource;
    private final List<String> allowedCorsHttpMethods;
    private static final Pattern gatewayRoutesPattern = Pattern.compile("apiml\\.routes.*.gateway\\S*");

    protected void checkInstanceInfo(InstanceInfo instanceInfo) {
        Map<String, String> metadata = instanceInfo.getMetadata();

        if (metadata != null && corsEnabled) {
            String serviceId = instanceInfo.getVIPAddress();
            setCorsConfiguration(serviceId, metadata);
        }
    }

    public void setCorsConfiguration(String serviceId, Map<String, String> metadata) {
        String isCorsEnabledForService = metadata.get("apiml.corsEnabled");
        if (Boolean.parseBoolean(isCorsEnabledForService)) {
            if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
                UrlBasedCorsConfigurationSource cors = (UrlBasedCorsConfigurationSource) this.corsConfigurationSource;
                final CorsConfiguration config = new CorsConfiguration();
                setAllowedOriginsForService(metadata, config);
                metadata.entrySet().stream()
                    .filter(entry -> gatewayRoutesPattern.matcher(entry.getKey()).find())
                    .forEach(entry ->
                        cors.registerCorsConfiguration("/" + entry.getValue() + "/" + serviceId.toLowerCase() + "/**", config));
            }
        }
    }

    private void setAllowedOriginsForService(Map<String, String> metadata, CorsConfiguration config) {
        // Check if the configuration specifies allowed origins for this service
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
}
