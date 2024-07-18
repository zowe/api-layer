/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
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
     * If there is already existing record, it will be updated
     *
     * @param user     User being routed towards southbound service
     * @param service  Service towards which is the user routed
     * @param loadBalancerCacheRecord  Object containing the selected instance and its creation time
     * @return True if storing succeeded, otherwise false
     */
    public Mono<Void> store(String user, String service, LoadBalancerCacheRecord loadBalancerCacheRecord) {
        if (remoteCache != null) {
            storeToRemoteCache(user, service, loadBalancerCacheRecord);
        }
        localCache.put(getKey(user, service), loadBalancerCacheRecord);
        log.debug("Stored record to local cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
        return true;
    }

    private Mono<Void> storeToRemoteCache(String user, String service, LoadBalancerCacheRecord loadBalancerCacheRecord) {
        try {
        String serializedRecord = mapper.writeValueAsString(loadBalancerCacheRecord);
        CachingServiceClient.KeyValue toStore = new CachingServiceClient.KeyValue(getKey(user, service), serializedRecord);
            createToRemoteCache(user, service, loadBalancerCacheRecord, toStore);
        } catch (JsonProcessingException e) {
            log.debug("Failed to serialize record for user: {}, service: {}, record {},  with exception: {}", user, service, loadBalancerCacheRecord, e);
        }
    }

    private Mono<Void> createToRemoteCache(String user, String service, LoadBalancerCacheRecord loadBalancerCacheRecord, CachingServiceClient.KeyValue toStore) {
        try {
        remoteCache.create(toStore);
        log.debug("Created record to remote cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
        } catch (CachingServiceClientException createException) {
            if (isCausedByCacheConflict(createException)) {
                updateToRemoteCache(user, service, loadBalancerCacheRecord, toStore);
            } else {
                log.debug("Failed to create record for user: {}, service: {}, record {}, with exception: {}", user, service, loadBalancerCacheRecord, createException);
            }
        }
    }

    private Mono<Void> updateToRemoteCache(String user, String service, LoadBalancerCacheRecord loadBalancerCacheRecord, CachingServiceClient.KeyValue toStore) {
        try {
            remoteCache.update(toStore);
            log.debug("Updated record to remote cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
        } catch (CachingServiceClientException updateException) {
            log.debug("Failed to update record for user: {}, service: {}, record {}, with exception: {}", user, service, loadBalancerCacheRecord, updateException);
        }
    }

    private boolean isCausedByCacheConflict(CachingServiceClientException e) {
        return e.getCause() instanceof HttpClientErrorException &&
            ((HttpClientErrorException) e.getCause()).getStatusCode().equals(HttpStatus.CONFLICT);
    }

    /**
     * Retrieve information about selected instance for combination of User and Service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     * @return Retrieved record containing the instance to use for this user and its creation time.
     */
    public Mono<LoadBalancerCacheRecord> retrieve(String user, String service) {
        if (remoteCache != null) {
            try {
                CachingServiceClient.KeyValue kv = remoteCache.read(getKey(user, service));
                if (kv != null) {
                    LoadBalancerCacheRecord loadBalancerCacheRecord = mapper.readValue(kv.getValue(), LoadBalancerCacheRecord.class);
                    log.debug("Retrieved record from remote cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
                    return loadBalancerCacheRecord;
                }
            } catch (CachingServiceClientException e) {
                log.debug("Failed to retrieve record for user: {}, service: {}, with exception: {}", user, service, e);
            } catch (JsonProcessingException e) {
                log.debug("Failed to deserialize record for user: {}, service: {}, with exception: {}", user, service, e);
            }
        }
        LoadBalancerCacheRecord loadBalancerCacheRecord = localCache.get(getKey(user, service));
        log.debug("Retrieved record from local cache for user: {}, service: {}, record: {}", user, service, loadBalancerCacheRecord);
        return loadBalancerCacheRecord;
    }

    // /**
    //  * Delete information stored for given user and service.
    //  *
    //  * @param user    User being routed towards southbound service
    //  * @param service Service towards which is the user routed
    //  */
    // public Mono<Void> delete(String user, String service) {
    //     if (remoteCache != null) {
    //         try {
    //             remoteCache.delete(getKey(user, service));
    //             log.debug("Deleted record from remote cache for user: {}, service: {}", user, service);
    //         } catch (CachingServiceClientException e) {
    //             log.debug("Failed to deleted record from remote cache for user: {}, service: {}, with exception: {}", user, service, e);
    //         }
    //     }
    //     localCache.remove(getKey(user, service));
    //     log.debug("Deleted record from local cache for user: {}, service: {}", user, service);
    // }

    private String getKey(String user, String service) {
        return LOAD_BALANCER_KEY_PREFIX + user + ":" + service;
    }

    /**
     * Data POJO that represents entry in load balancing service cache
     */
    @Data
    public class LoadBalancerCacheRecord {
        private final String instanceId;
        private final LocalDateTime creationTime;

        public LoadBalancerCacheRecord(String instanceId) {
            this(instanceId, LocalDateTime.now());
        }

        @JsonCreator
        public LoadBalancerCacheRecord(
                @JsonProperty("instanceId") String instanceId,
                @JsonProperty("creationTime") LocalDateTime creationTime) {
            this.instanceId = instanceId;
            this.creationTime = creationTime;
        }
    }

}
