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

import java.util.List;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * service class offered methods for checking onboarding information and also retrieve metadata from
 * provided serviceid.
 */
@Service
@RequiredArgsConstructor
public class VerificationOnboardService {

    private final DiscoveryClient discoveryClient;

    /**
     * Accept serviceId and check if the service is onboarded to the API Mediation Layer
     * @param serviceId accept serviceId to check
     * @return return true if the service is known by Eureka otherwise false.
     */
    public boolean checkOnboarding(String serviceId) {
        
        List<String> serviceLists = discoveryClient.getServices();
        return serviceLists.contains(serviceId);

    }

    /**
     * Accept serviceId and check if the 
     * @param serviceId accept serviceId to check
     * @return return swagger Url if the metadata can be retrieved, otherwise an empty string.
     */
    public String retrieveMetaData(String serviceId) {

        String swaggerUrl = "";
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
        ServiceInstance serviceInstance = serviceInstances.get(0);
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata.containsKey("apiml.apiInfo.api-v2.swaggerUrl")) {
            swaggerUrl = metadata.get("apiml.apiInfo.api-v2.swaggerUrl");
        }
            
        return swaggerUrl;

    }   
}