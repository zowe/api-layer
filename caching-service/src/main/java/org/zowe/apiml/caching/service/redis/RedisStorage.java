/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.redis.exceptions.RedisOutOfMemoryException;
import org.zowe.apiml.caching.service.redis.exceptions.RetryableRedisException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class handles requests from controller and orchestrates operations on the low level RedisOperator class.
 * <p>
 * Storage eviction and maximum memory usage is left to Redis configuration. If an entry will take more than the maximum
 * configured memory, or there is not enough memory available and a no eviction policy is used, an error message is returned to the user.
 * If another entry will be evicted to make space for a create or update operation, no warning is logged and the eviction
 * is left to Redis.
 */
@Slf4j
public class RedisStorage implements Storage {
    private final RedisOperator redis;

    public RedisStorage(RedisOperator redisOperator) {
        log.info("Using Redis for the cached data");

        this.redis = redisOperator;
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue create(String serviceId, KeyValue toCreate) {
        log.info("Creating entry: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());

        RedisEntry entryToCreate = new RedisEntry(serviceId, toCreate);
        try {
            boolean result = redis.create(entryToCreate);

            if (!result) {
                throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey(), serviceId);
            }
        } catch (RedisOutOfMemoryException e) {
            throw new StorageException(Messages.INSUFFICIENT_STORAGE.getKey(), Messages.INSUFFICIENT_STORAGE.getStatus());
        }
        return toCreate;
    }

    @Override
    public KeyValue storeMapItem(String serviceId, String mapKey, KeyValue toCreate) throws StorageException {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    public Map<String, String> getAllMapItems(String serviceId, String mapKey) throws StorageException {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    public Map<String, Map<String, String>> getAllMaps(String serviceId) throws StorageException {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue read(String serviceId, String key) {
        log.info("Reading entry: {}|{}", serviceId, key);

        RedisEntry result = redis.get(serviceId, key);
        if (result == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }
        return result.getEntry();
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        log.info("Updating entry: {}|{}|{}", serviceId, toUpdate.getKey(), toUpdate.getValue());

        RedisEntry entryToUpdate = new RedisEntry(serviceId, toUpdate);
        try {
            boolean result = redis.update(entryToUpdate);

            if (!result) {
                throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toUpdate.getKey(), serviceId);
            }
        } catch (RedisOutOfMemoryException e) {
            throw new StorageException(Messages.INSUFFICIENT_STORAGE.getKey(), Messages.INSUFFICIENT_STORAGE.getStatus());
        }
        return toUpdate;
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue delete(String serviceId, String toDelete) {
        log.info("Deleting entry: {}|{}", serviceId, toDelete);

        RedisEntry entryToDelete = redis.get(serviceId, toDelete);
        boolean result = redis.delete(serviceId, toDelete);

        if (!result) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toDelete, serviceId);
        }
        return entryToDelete.getEntry();
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public Map<String, KeyValue> readForService(String serviceId) {
        log.info("Reading all entries: {}", serviceId);

        List<RedisEntry> redisResult = redis.get(serviceId);
        Map<String, KeyValue> readResult = new HashMap<>();

        for (RedisEntry redisEntry : redisResult) {
            readResult.put(redisEntry.getEntry().getKey(), redisEntry.getEntry());
        }
        return readResult;
    }

    @Override
    public void deleteForService(String serviceId) {
        log.info("Deleting all entries: {}", serviceId);

        boolean result = redis.delete(serviceId);

        if (!result) {
            log.info("No entries were deleted for {}", serviceId);
        }
    }

    @Override
    public void removeNonRelevantTokens(String serviceId, String mapKey) {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Override
    public void removeNonRelevantRules(String serviceId, String mapKey) {
        throw new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }
}
