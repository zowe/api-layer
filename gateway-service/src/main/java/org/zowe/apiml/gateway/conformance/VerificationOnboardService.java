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
import org.springframework.cloud.client.ServiceInstance;
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
     * Accepts serviceId and checks if the metadata field exists and is not empty
     *
     * @param serviceId serviceId to check
     * @return true when it can retrieve metadata, false otherwise.
     */
    public boolean canRetrieveMetaData(String serviceId) {

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);

        if (!serviceInstances.isEmpty()) {
            ServiceInstance serviceInstance = serviceInstances.get(0);
            Map<String, String> metadata = serviceInstance.getMetadata();
            return metadata != null && !metadata.isEmpty();
        }
        return false;
    }



    /**
     * Accepts serviceId and retrieves the Swagger url if it exists
     *
     * @param serviceId serviceId to check
     * @return SwaggerUrl when able, empty string otherwise
     */
    public String retrieveSwagger(String serviceId) {

        if (!canRetrieveMetaData(serviceId)) {
            return "";
        }
        ServiceInstance serviceInstance = discoveryClient.getInstances(serviceId).get(0);
        Map<String, String> metadata = serviceInstance.getMetadata();
        String swaggerUrl = metadata.get("apiml.apiInfo.api-v2.swaggerUrl");
        if (swaggerUrl != null) {
            return swaggerUrl;
        }

        return "";
    }


}
