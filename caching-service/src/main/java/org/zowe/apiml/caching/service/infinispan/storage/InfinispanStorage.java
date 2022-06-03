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
import org.infinispan.AdvancedCache;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;

import javax.transaction.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class InfinispanStorage implements Storage {


    private final ConcurrentMap<String, KeyValue> cache;
    private final ConcurrentMap<String, List<String>> tokenCache;

    public InfinispanStorage(ConcurrentMap<String, KeyValue> cache, ConcurrentMap<String, List<String>> tokenCache) {
        this.cache = cache;
        this.tokenCache = tokenCache;
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
    public KeyValue storeInvalidatedToken(String serviceId, KeyValue toCreate) {
        try {
            TransactionManager tm = ((AdvancedCache)tokenCache).getAdvancedCache().getTransactionManager();
            tm.begin();
            if (!tokenCache.get(serviceId + toCreate.getKey()).contains(toCreate.getValue())) {
                log.info("Storing the invalidated token: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());

                List<String> tokensList = tokenCache.computeIfAbsent(serviceId + toCreate.getKey(), k -> new ArrayList<>());
                tokensList.add(toCreate.getValue());
                tokenCache.put(serviceId + toCreate.getKey(), tokensList);
            }
            tm.commit();
        } catch (NotSupportedException| SystemException | HeuristicRollbackException | HeuristicMixedException | RollbackException e) {
            throw new StorageException(Messages.INTERNAL_SERVER_ERROR.getKey(), Messages.INTERNAL_SERVER_ERROR.getStatus(), toCreate.getKey());
        }
        return null;
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
}
