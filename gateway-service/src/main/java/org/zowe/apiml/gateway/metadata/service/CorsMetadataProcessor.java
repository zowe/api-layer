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
import org.apache.logging.log4j.util.Strings;
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
        String serviceId = instanceInfo.getVIPAddress();
        Map<String, String> metadata = instanceInfo.getMetadata();
        if (metadata != null) {
            setCorsConfiguration(serviceId, metadata);
        }
    }

    public void setCorsConfiguration(String serviceId, Map<String, String> metadata) {
        String corsConfiguration = metadata.get("apiml.corsConfiguration");
        if (!Strings.isEmpty(corsConfiguration)) {
            if (this.corsConfigurationSource instanceof UrlBasedCorsConfigurationSource) {
                UrlBasedCorsConfigurationSource cors = (UrlBasedCorsConfigurationSource) this.corsConfigurationSource;
                final CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
                config.setAllowCredentials(true);
                config.setAllowedMethods(allowedCorsHttpMethods);
                cors.registerCorsConfiguration("/api/v1/" + serviceId.toLowerCase() + "/*", config);
            }

        }
    }
}
