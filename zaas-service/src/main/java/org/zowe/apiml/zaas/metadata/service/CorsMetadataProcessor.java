/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.metadata.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.util.CorsUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Order(15)
public class CorsMetadataProcessor extends MetadataProcessor {

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;
    private final EurekaApplications applications;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CorsUtils corsUtils;


    @Override
    List<Application> getApplications() {
        return this.applications.getRegistered();
    }

    protected void checkInstanceInfo(InstanceInfo instanceInfo) {
        Map<String, String> metadata = instanceInfo.getMetadata();

        if (metadata != null && corsEnabled) {
            UrlBasedCorsConfigurationSource cors = (UrlBasedCorsConfigurationSource) this.corsConfigurationSource;
            corsUtils.setCorsConfiguration(instanceInfo.getVIPAddress().toLowerCase(), metadata, (entry, serviceId, config) -> cors.registerCorsConfiguration("/" + entry + "/" + serviceId + "/**", config));
        }
    }
}
