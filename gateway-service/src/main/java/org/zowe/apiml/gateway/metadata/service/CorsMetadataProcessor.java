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
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CorsMetadataProcessor extends MetadataProcessor {

    private final EurekaApplications applications;
    private final CorsConfigurationSource corsConfigurationSource;
    private final List<String> allowedCorsHttpMethods;

    @Override
    List<Application> getApplications() {
        return this.applications.getRegistered();
    }

    protected void checkInstanceInfo(InstanceInfo instanceInfo) {
        Map<String, String> metadata = instanceInfo.getMetadata();

        if (metadata != null) {
            String serviceId = instanceInfo.getVIPAddress();
            setCorsConfiguration(serviceId, metadata);
        }
    }

    public void setCorsConfiguration(String serviceId, Map<String, String> metadata) {
        String isCorsEnabled = metadata.get("apiml.corsEnabled");
        if (Boolean.parseBoolean(isCorsEnabled)) {
            if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
                UrlBasedCorsConfigurationSource cors = (UrlBasedCorsConfigurationSource) this.corsConfigurationSource;
                final CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.addAllowedOrigin(CorsConfiguration.ALL);
                config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
                config.setAllowedMethods(allowedCorsHttpMethods);
                metadata.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("apiml.routes"))
                    .forEach(entry ->
                        cors.registerCorsConfiguration("/" + serviceId + entry.getValue(), config));
            }
            System.out.println("");
        }
    }
}
