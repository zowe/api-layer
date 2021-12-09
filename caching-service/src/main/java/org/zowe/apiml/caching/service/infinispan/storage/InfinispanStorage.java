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

import lombok.RequiredArgsConstructor;
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

    private final DefaultCacheManager defaultCacheManager;
    private Cache<Object,Object> cache;

    public InfinispanStorage(DefaultCacheManager defaultCacheManager) {
        this.defaultCacheManager = defaultCacheManager;
        cache = defaultCacheManager.getCache("myCache");
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Writing record: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());
        cache.computeIfAbsent(serviceId, k -> new HashMap<>());
        Map<String, KeyValue> serviceEntries = (Map<String, KeyValue>) cache.get(serviceId);
        if(serviceEntries.containsKey(toCreate.getKey())){
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey());
        }

        Object o = cache.putIfAbsent(toCreate.getKey(), toCreate);
        return toCreate;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        return (KeyValue) defaultCacheManager.getCache("myCache").get(key);
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        defaultCacheManager.getCache("myCache").put(toUpdate.getKey(), toUpdate);
        return

            null;
    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        return (KeyValue) defaultCacheManager.getCache("myCache").remove(toDelete);
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        return null;
    }

    @Override
    public void deleteForService(String serviceId) {

    }
}
