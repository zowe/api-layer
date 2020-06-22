/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.metadata.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CorsMetadataProcessor extends MetadataProcessor {

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;

    private final EurekaApplications applications;
    private final CorsConfigurationSource corsConfigurationSource;
    private final List<String> allowedCorsHttpMethods;
    private static final Pattern gatewayRoutesPattern = Pattern.compile("apiml\\.routes.*.gateway\\S*");


    @Override
    List<Application> getApplications() {
        return this.applications.getRegistered();
    }

    protected void checkInstanceInfo(InstanceInfo instanceInfo) {
        Map<String, String> metadata = instanceInfo.getMetadata();

        if (metadata != null && corsEnabled) {
            String serviceId = instanceInfo.getVIPAddress();
            setCorsConfiguration(serviceId, metadata);
        }
    }

    public void setCorsConfiguration(String serviceId, Map<String, String> metadata) {
        String isCorsEnabledForService = metadata.get("apiml.corsEnabled");
        if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
            UrlBasedCorsConfigurationSource cors = (UrlBasedCorsConfigurationSource) this.corsConfigurationSource;
            final CorsConfiguration config = new CorsConfiguration();
            if (Boolean.parseBoolean(isCorsEnabledForService)) {
                config.setAllowCredentials(true);
                config.addAllowedOrigin(CorsConfiguration.ALL);
                config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
                config.setAllowedMethods(allowedCorsHttpMethods);
            }
            metadata.entrySet().stream()
                .filter(entry -> gatewayRoutesPattern.matcher(entry.getKey()).find())
                .forEach(entry ->
                    cors.registerCorsConfiguration("/" + entry.getValue() + "/" + serviceId.toLowerCase() + "/**", config));
        }
    }
}
