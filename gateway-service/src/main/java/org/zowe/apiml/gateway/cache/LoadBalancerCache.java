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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private final Map<String, LoadBalancerCacheRecord> localCache;
    private final CachingServiceClient remoteCache;
    private final ObjectMapper mapper = new ObjectMapper();

    public static final String loadBalancerKeyPrefix = "lb.";

    public LoadBalancerCache(CachingServiceClient cachingServiceClient) {
        this.remoteCache = cachingServiceClient;
        localCache = new ConcurrentHashMap<>();
        mapper.registerModule(new JavaTimeModule());
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
            if (remoteCache != null) {
                try {
                    remoteCache.create(new CachingServiceClient.KeyValue(getKey(user, service), mapper.writeValueAsString(loadBalancerCacheRecord)));
                } catch (CachingServiceClient.CachingServiceClientException e) {
                    // TODO what are the rules for this case?
                    // TODO Logging at least
                    e.printStackTrace();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            localCache.put(getKey(user, service), loadBalancerCacheRecord);
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
        if (remoteCache != null) {
            try {
                CachingServiceClient.KeyValue kv = remoteCache.read(getKey(user, service));
                if (kv != null) {
                    //TODO not a json here, stored as String
                    return mapper.readValue(kv.getValue(), LoadBalancerCacheRecord.class);
                }
            } catch (CachingServiceClient.CachingServiceClientException | JsonProcessingException e) {
                // TODO what are the rules for this case?
                e.printStackTrace();
            }
        }
        return localCache.get(getKey(user, service));
    }

    /**
     * Delete information stored for given user and service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     */
    public synchronized void delete(String user, String service) {
        if (remoteCache != null) {
            try {
                remoteCache.delete(getKey(user, service));
            } catch (CachingServiceClient.CachingServiceClientException e) {
                // TODO what are the rules for this case?
                e.printStackTrace();
            }
        }
        localCache.remove(getKey(user, service));
    }

    private String getKey(String user, String service) {
        return loadBalancerKeyPrefix + user + ":" + service;
    }
}
