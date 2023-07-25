/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service class that offers methods for checking onboarding information and also checks availability metadata from
 * a provided serviceId.
 */
@Service
@RequiredArgsConstructor
public class VerificationOnboardService {

    private final DiscoveryClient discoveryClient;

    /**
     * Accepts serviceId and checks if the service is onboarded to the API Mediation Layer
     *
     * @param serviceId serviceId to check
     * @return true if the service is known by Eureka otherwise false.
     */
    public boolean checkOnboarding(String serviceId) {

        List<String> serviceLists = discoveryClient.getServices();

        return serviceLists.contains(serviceId);

    }


    /**
     * Accepts metadata and retrieves the Swagger url if it existsd
     *
     * @param metadata to grab swagger from
     * @return SwaggerUrl when able, empty string otherwise
     */
    public String retrieveSwagger(Map<String, String> metadata) {
        String swaggerUrl = metadata.get("apiml.apiInfo.api-v2.swaggerUrl");
        if (swaggerUrl != null) {
            return swaggerUrl;
        }
        return "";
    }


}
