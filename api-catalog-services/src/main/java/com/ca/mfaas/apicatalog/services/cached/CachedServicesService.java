/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.cached;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for eureka services
 */
@Slf4j
@Service
public class CachedServicesService {

    private Map<String, Application> services = new HashMap<>();
    private long versionDelta;

    /**
     * return all cached service instances
     * @return instances
     */
    public Applications getAllCachedServices() {
        if (services.isEmpty()) {
            return null;
        } else {
            return new Applications(null, 1L, new ArrayList<>(services.values()));
        }
    }

    /**
     * return all cached service instances
     * @param serviceId the service identifier
     * @return instances for this service (might be empty instances collection)
     */
    public Application getService(@NonNull final String serviceId) {
        return services.get(serviceId.toLowerCase());
    }

    /**
     * Update this service with the application object
     * @param serviceId the service name (lowercase)
     * @param application updated application with running instances
     */
    public void updateService(@NonNull final String serviceId, final Application application) {
        services.put(serviceId.toLowerCase(), application);
    }

    /**
     * Clear the cache and remove all entries from the map
     */
    public void clearAllServices() {
        services.clear();
    }

    public long getVersionDelta() {
        return versionDelta;
    }

    public void setVersionDelta(long versionDelta) {
        this.versionDelta = versionDelta;
    }

    public InstanceInfo getInstanceInfoForService(String serviceId) {
        Application service = services.get(serviceId.toLowerCase());
        if (service != null && service.getInstances() != null && !service.getInstances().isEmpty()) {
            log.warn("Service: " + serviceId + " contains no cached instances.");
            return service.getInstances().get(0);
        }
        return null;
    }
}
