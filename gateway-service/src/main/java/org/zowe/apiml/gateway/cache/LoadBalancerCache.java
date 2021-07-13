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

import lombok.Getter;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for storing Load Balancer related information. The initial goal was to support user based instance load
 * balancing
 */
@Getter
public class LoadBalancerCache {
    private final Map<String, LoadBalancerCacheRecord> cache;

    public LoadBalancerCache() {
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Store information about instance the user is balanced towards.
     *
     * @param user     User being routed towards southbound service
     * @param service  Service towards which is the user routed
     * @param loadBalancerCacheRecord  Object containing the selected instance and its creation time
     * @return True if storing succeeded, otherwise false
     */
    public synchronized boolean store(String user, String service, LoadBalancerCacheRecord loadBalancerCacheRecord) {
        try {
            cache.put(getKey(user, service), loadBalancerCacheRecord);
            return true;
        } catch (UnsupportedOperationException | ClassCastException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Retrieve information about selected instance for combination of User and Service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     * @return Retrieved record containing the instance to use for this user and its creation time.
     */
    public synchronized LoadBalancerCacheRecord retrieve(String user, String service) {
        return cache.get(getKey(user, service));
    }

    /**
     * Delete information stored for given user and service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     */
    public synchronized void delete(String user, String service) {
        cache.remove(getKey(user, service));
    }

    private String getKey(String user, String service) {
        return user + ":" + service;
    }
}
