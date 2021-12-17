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

import lombok.extern.slf4j.Slf4j;
import org.infinispan.Cache;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InfinispanStorage implements Storage {


    private final Cache<String, Map<String, KeyValue>> cache;

    public InfinispanStorage(Cache<String, Map<String, KeyValue>> cache) {
        this.cache = cache;
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());

        Map<String, KeyValue> serviceCache = cache.computeIfAbsent(serviceId, k -> new HashMap<>());

        if (serviceCache.containsKey(toCreate.getKey())) {
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey());
        }
        KeyValue entry = serviceCache.put(toCreate.getKey(), toCreate);
        cache.put(serviceId, serviceCache);
        return entry;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        log.info("Reading record for service {} under key {}", serviceId, key);
        Map<String, KeyValue> serviceCache = cache.get(serviceId);
        if (serviceCache != null && serviceCache.containsKey(key)) {
            return serviceCache.get(key);
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        log.info("Updating record for service {} under key {}", serviceId, toUpdate);
        Map<String, KeyValue> serviceCache = cache.get(serviceId);
        if (serviceCache == null || !serviceCache.containsKey(toUpdate.getKey())) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toUpdate.getKey(), serviceId);
        }
        serviceCache.put(toUpdate.getKey(), toUpdate);
        cache.put(serviceId, serviceCache);
        return toUpdate;

    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        log.info("Removing record for service {} under key {}", serviceId, toDelete);
        Map<String, KeyValue> serviceCache = cache.get(serviceId);
        KeyValue entry;
        if (serviceCache.containsKey(toDelete)) {
            entry = serviceCache.remove(toDelete);
            cache.put(serviceId, serviceCache);
            return entry;
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toDelete, serviceId);
        }
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        log.info("Reading all records for service {} ", serviceId);
        return cache.get(serviceId);
    }

    @Override
    public void deleteForService(String serviceId) {
        log.info("Removing all records for service {} ", serviceId);
        cache.remove(serviceId);
    }
}
