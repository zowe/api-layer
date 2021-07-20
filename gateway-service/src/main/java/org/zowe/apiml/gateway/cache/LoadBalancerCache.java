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
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for storing Load Balancer related information. The initial goal was to support user based instance load
 * balancing
 *
 * Supports optional CachingServiceClient inject through constructor, which gives acts as remote cache. Remote cache
 * entries have preference to local ones.
 */
@Getter
@Slf4j
public class LoadBalancerCache {
    private final Map<String, LoadBalancerCacheRecord> localCache;
    private final CachingServiceClient remoteCache;
    private final ObjectMapper mapper = new ObjectMapper();

    public static final String LOAD_BALANCER_KEY_PREFIX = "lb.";

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
    public boolean store(String user, String service, LoadBalancerCacheRecord loadBalancerCacheRecord) {
        if (remoteCache != null) {
            try {
                remoteCache.create(new CachingServiceClient.KeyValue(getKey(user, service), mapper.writeValueAsString(loadBalancerCacheRecord)));
                log.debug("Stored record to remote cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
            } catch (CachingServiceClient.CachingServiceClientException e) {
                log.debug("Failed to store record for user: {}, service: {}, record {}, with exception: {}", user, service, loadBalancerCacheRecord, e);
            } catch (JsonProcessingException e) {
                log.debug("Failed to serialize record for user: {}, service: {}, record {},  with exception: {}", user, service, loadBalancerCacheRecord, e);
            }
        }
        localCache.put(getKey(user, service), loadBalancerCacheRecord);
        log.debug("Stored record to local cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
        return true;
    }

    /**
     * Retrieve information about selected instance for combination of User and Service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     * @return Retrieved record containing the instance to use for this user and its creation time.
     */
    public LoadBalancerCacheRecord retrieve(String user, String service) {
        if (remoteCache != null) {
            try {
                CachingServiceClient.KeyValue kv = remoteCache.read(getKey(user, service));
                if (kv != null) {
                    LoadBalancerCacheRecord loadBalancerCacheRecord = mapper.readValue(kv.getValue(), LoadBalancerCacheRecord.class);
                    log.debug("Retrieved record from remote cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
                    return loadBalancerCacheRecord;
                }
            } catch (CachingServiceClient.CachingServiceClientException e) {
                log.debug("Failed to retrieve record for user: {}, service: {}, with exception: {}", user, service, e);
            } catch (JsonProcessingException e) {
                log.debug("Failed to deserialize record for user: {}, service: {}, with exception: {}", user, service, e);
            }
        }
        LoadBalancerCacheRecord loadBalancerCacheRecord = localCache.get(getKey(user, service));
        log.debug("Retrieved record from local cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
        return loadBalancerCacheRecord;
    }

    /**
     * Delete information stored for given user and service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     */
    public void delete(String user, String service) {
        if (remoteCache != null) {
            try {
                remoteCache.delete(getKey(user, service));
                log.debug("Deleted record from remote cache for user: {}, service: {}", user, service);
            } catch (CachingServiceClient.CachingServiceClientException e) {
                log.debug("Failed to deleted record from remote cache for user: {}, service: {}, with exception: {}", user, service, e);
            }
        }
        localCache.remove(getKey(user, service));
        log.debug("Deleted record from local cache for user: {}, service: {}", user, service);
    }

    private String getKey(String user, String service) {
        return LOAD_BALANCER_KEY_PREFIX + user + ":" + service;
    }
}
