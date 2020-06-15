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

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.ribbon.ApimlZoneAwareLoadBalancer;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class is responsible for evicting cache after new registry is loaded. This avoid race condition. Scenario is:
 * 1. discovery service changed
 * a. about change is notified all gateways
 * b. gateways evict caches and start with mirroring of discovery into discoveryClient
 * 2. now is possible cache again data with old settings from discovery service, because fetching new is asynchronous
 * 3. after make fetching this beans is notified from discovery client and evict caches again
 * <p>
 * This process evict evict caches two times, because not all reason to cache is dependent only by discovery client
 * updates.
 */
@Component
@Slf4j
public class ServiceCacheEvictor implements EurekaEventListener, ServiceCacheEvict {

    private List<ServiceCacheEvict> serviceCacheEvicts;

    private boolean evictAll = false;
    private HashSet<ServiceRef> toEvict = new HashSet<>();
    private LinkedBlockingQueue<String> loadBalancerIDsForRefresh = new LinkedBlockingQueue<>();
    private Map<String, ApimlZoneAwareLoadBalancer> apimlZoneAwareLoadBalancer = new ConcurrentHashMap<>();

    public ServiceCacheEvictor(
        ApimlDiscoveryClient apimlDiscoveryClient,
        List<ServiceCacheEvict> serviceCacheEvicts
    ) {
        apimlDiscoveryClient.registerEventListener(this);
        this.serviceCacheEvicts = serviceCacheEvicts;
        this.serviceCacheEvicts.remove(this);
    }

    public void addApimlZoneAwareLoadBalancer(ApimlZoneAwareLoadBalancer apimlZoneAwareLoadBalancer) {
        String loadBalancerName = apimlZoneAwareLoadBalancer.getName();
        this.apimlZoneAwareLoadBalancer.put(loadBalancerName, apimlZoneAwareLoadBalancer);
    }

    public void enqueueLoadBalancer(String loadBalancerID) {
        try {
            loadBalancerIDsForRefresh.put(loadBalancerID);
        } catch (InterruptedException e) {
            log.error("Error enqueuing load balancer ID for refresh " + e.getMessage());
        }
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
    public synchronized void onEvent(EurekaEvent event) {
        if (event instanceof CacheRefreshedEvent) {
            if (!evictAll && toEvict.isEmpty()) return;
            if (evictAll) {
                serviceCacheEvicts.forEach(ServiceCacheEvict::evictCacheAllService);
                apimlZoneAwareLoadBalancer.values().forEach(ApimlZoneAwareLoadBalancer::updateListOfServers);
                evictAll = false;
                return;
            } else {
                toEvict.forEach(ServiceRef::evict);
                toEvict.clear();
            }
            updateCorrectLoadBalancer();
        }
    }

    private void updateCorrectLoadBalancer() {
        while (!loadBalancerIDsForRefresh.isEmpty()) {
            String loadBalancerId = loadBalancerIDsForRefresh.poll();
            updateLoadBalancer(loadBalancerId);
        }
    }

    private void updateLoadBalancer(String loadBalancerId) {
        ApimlZoneAwareLoadBalancer loadBalancer = apimlZoneAwareLoadBalancer.get(loadBalancerId);
        if (loadBalancer != null) {
            loadBalancer.updateListOfServers();
        }
    }

    @Value
    private class ServiceRef {

        private final String serviceId;

        public void evict() {
            serviceCacheEvicts.forEach(x -> x.evictCacheService(serviceId));
        }


    }

}
