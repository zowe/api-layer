/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.cache;

import lombok.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.metadata.service.RefreshEventListener;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;

import java.util.HashSet;
import java.util.List;

/**
 * This class is responsible for evicting cache after new registry is loaded. This avoid race condition. Scenario is:
 * 1. discovery service changed
 * a. about change is notified all gateways
 * b. gateways evict caches and start with mirroring of discovery into discoveryClient
 * 2. now is possible cache again data with old settings from discovery service, because fetching new is asynchronous
 * 3. after make fetching this beans is notified from discovery client and evict caches again
 * This process evict evict caches two times, because not all reason to cache is dependent only by discovery client
 * updates.
 */
@Component
public class ServiceCacheEvictor extends RefreshEventListener implements ServiceCacheEvict {

    private List<ServiceCacheEvict> serviceCacheEvicts;

    private boolean evictAll = false;
    private HashSet<ServiceRef> toEvict = new HashSet<>();

    public ServiceCacheEvictor(
        List<ServiceCacheEvict> serviceCacheEvicts
    ) {
        this.serviceCacheEvicts = serviceCacheEvicts;
        this.serviceCacheEvicts.remove(this);
    }


    public synchronized void evictCacheService(String serviceId) {
        if (evictAll) return;
        toEvict.add(new ServiceRef(serviceId));
    }

    public synchronized void evictCacheAllService() {
        evictAll = true;
        toEvict.clear();
    }

    @Override
    public synchronized void refresh() {
        if (!evictAll && toEvict.isEmpty()) return;
        if (evictAll) {
            serviceCacheEvicts.forEach(ServiceCacheEvict::evictCacheAllService);
            evictAll = false;
        } else {
            toEvict.forEach(ServiceRef::evict);
            toEvict.clear();
        }
    }


    @Value
    private class ServiceRef {

        String serviceId;

        public void evict() {
            serviceCacheEvicts.forEach(x -> x.evictCacheService(serviceId));
        }


    }

}
