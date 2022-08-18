/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.infinispan.lock.api.ClusteredLock;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.models.AccessTokenContainer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class InfinispanStorage implements Storage {


    private final ConcurrentMap<String, KeyValue> cache;
    private final ConcurrentMap<String, Map<String, String>> tokenCache;
    private final ClusteredLock lock;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public InfinispanStorage(ConcurrentMap<String, KeyValue> cache, ConcurrentMap<String, Map<String, String>> tokenCache, ClusteredLock lock) {
        this.cache = cache;
        this.tokenCache = tokenCache;
        this.lock = lock;
    }

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        toCreate.setServiceId(serviceId);
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());

        KeyValue serviceCache = cache.putIfAbsent(serviceId + toCreate.getKey(), toCreate);

        if (serviceCache != null) {
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey());
        }
        return null;
    }

    @Override
    public KeyValue storeMapItem(String serviceId, String mapKey, KeyValue toCreate) {
        CompletableFuture<Boolean> complete = lock.tryLock(4, TimeUnit.SECONDS).whenComplete((r, ex) -> {
            if (Boolean.TRUE.equals(r)) {
                try {
                    String cacheKey = serviceId + mapKey;
                    log.info("Storing the item into token cache: {} -> {}|{}", cacheKey, toCreate.getKey(), toCreate.getValue());
                    Map<String, String> tokenCacheItem = tokenCache.get(cacheKey);
                    if (tokenCacheItem == null) {
                        tokenCacheItem = new HashMap<>();
                    }
                    tokenCacheItem.put(toCreate.getKey(), toCreate.getValue());
                    tokenCache.put(cacheKey, tokenCacheItem);
                } finally {
                    lock.unlock();
                }
            }
        });
        try {
            complete.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof StorageException) {
                throw (StorageException) e.getCause();
            } else {
                log.error("Unexpected error while acquiring the lock ", e);
                throw e;
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getAllMapItems(String serviceId, String mapKey) {
        log.info("Reading all records from token cache for service {} under the {} key.", serviceId, mapKey);
        return tokenCache.get(serviceId + mapKey);
    }

    @Override
    public Map<String, Map<String, String>> getAllMaps(String serviceId) {
        log.info("Reading all records from token cache for service {} ", serviceId);
        // filter all maps which belong given service and remove the service name from key names.
        return tokenCache.entrySet().stream().filter(
            entry -> entry.getKey().startsWith(serviceId))
            .collect(Collectors.toMap(e -> e.getKey().substring(serviceId.length()), Map.Entry::getValue));
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        log.info("Reading record for service {} under key {}", serviceId, key);
        KeyValue serviceCache = cache.get(serviceId + key);
        if (serviceCache != null) {
            return serviceCache;
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        toUpdate.setServiceId(serviceId);
        log.info("Updating record for service {} under key {}", serviceId, toUpdate);
        KeyValue serviceCache = cache.put(serviceId + toUpdate.getKey(), toUpdate);
        if (serviceCache == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toUpdate.getKey(), serviceId);
        }
        return toUpdate;

    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        log.info("Removing record for service {} under key {}", serviceId, toDelete);
        KeyValue entry = cache.remove(serviceId + toDelete);
        if (entry != null) {
            return entry;
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toDelete, serviceId);
        }
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        log.info("Reading all records for service {} ", serviceId);
        Map<String, KeyValue> result = new HashMap<>();
        cache.forEach((key, value) -> {
            if (serviceId.equals(value.getServiceId())) {
                result.put(value.getKey(), value);
            }
        });
        return result;
    }

    @Override
    public void deleteForService(String serviceId) {
        log.info("Removing all records for service {} ", serviceId);
        cache.forEach((key, value) -> {
            if (value.getServiceId().equals(serviceId)) {
                cache.remove(key);
            }
        });
    }

    @Override
    public void deleteItemFromMap(String serviceId, String mapKey) {
        if (mapKey.equals("invalidUsers") || mapKey.equals("invalidScopes")) {
            removeNonRelevantRules(serviceId, mapKey);
        } else {
            removeNonRelevantTokens(serviceId, mapKey);
        }
    }

    private void removeNonRelevantTokens(String serviceId, String mapKey) {
        Map<String, String> map = tokenCache.get(serviceId + mapKey);
        if (map != null && !map.isEmpty()) {
            LocalDateTime timestamp = LocalDateTime.now();
            ConcurrentMap<String,String> concurrentMap = new ConcurrentHashMap<>(map);
            for (Map.Entry<String,String> entry : concurrentMap.entrySet()) {
                try {
                    AccessTokenContainer c = objectMapper.readValue(entry.getValue(), AccessTokenContainer.class);
                    if (c.getExpiresAt().isBefore(timestamp)) {
                        removeEntry(serviceId, mapKey, map, entry);
                    }

                } catch (JsonProcessingException e) {
                    log.error("Not able to parse invalidToken json value.", e);
                }
            }
        }
    }

    private void removeNonRelevantRules(String serviceId, String mapKey) {
        long timestamp = System.currentTimeMillis();
        Map<String, String> map = tokenCache.get(serviceId + mapKey);
        if (map != null && !map.isEmpty()) {
            ConcurrentMap<String, String> concurrentMap = new ConcurrentHashMap<>(map);
            for (Map.Entry<String, String> entry : concurrentMap.entrySet()) {
                long delta = timestamp - Long.parseLong(entry.getValue());
                long deltaToDays = TimeUnit.MILLISECONDS.toDays(delta);
                if (deltaToDays > 90) {
                    removeEntry(serviceId, mapKey, map, entry);
                }
            }
        }
    }

    private void removeEntry(String serviceId, String mapKey, Map<String, String> map, Map.Entry<String, String> entry) {
        CompletableFuture<Boolean> complete = lock.tryLock(4, TimeUnit.SECONDS).whenComplete((r, ex) -> {
                if (Boolean.TRUE.equals(r)) {
                    try {
                        log.info("Removing record from the cache under key {} ", entry.getKey());
                        map.remove(entry.getKey());
                        tokenCache.put(serviceId + mapKey, map);
                    } finally {
                        lock.unlock();
                    }
                }
        });
        try {
            complete.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof StorageException) {
                throw (StorageException) e.getCause();
            } else {
                log.error("Unexpected error while acquiring the lock ", e);
                throw e;
            }
        }
    }
}
