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
import org.infinispan.manager.DefaultCacheManager;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InfinispanStorage implements Storage {


    private final Cache<String, Map<String, KeyValue>> cache;

    public InfinispanStorage(DefaultCacheManager defaultCacheManager) {
        this.cache = defaultCacheManager.getCache("myCache");
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());

        Map<String, KeyValue> storage = cache.computeIfAbsent(serviceId, k -> new HashMap<>());

        if (storage.containsKey(toCreate.getKey())) {
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey());
        }
        KeyValue entry = storage.put(toCreate.getKey(), toCreate);
        cache.put(serviceId, storage);
        return entry;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        Map<String, KeyValue> serviceCache = cache.get(serviceId);
        if (serviceCache != null && serviceCache.containsKey(key)) {
            return serviceCache.get(key);
        } else {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
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
        return cache.get(serviceId);
    }

    @Override
    public void deleteForService(String serviceId) {
        cache.remove(serviceId);
    }
}
