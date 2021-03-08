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
import org.zowe.apiml.caching.service.redis.config.RedisConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Class handles requests from controller and orchestrates operations on the low level RedisOperator class
 */
@Slf4j
public class RedisStorage implements Storage {
    private final RedisConfig config;
    private final RedisOperator redis;

    public RedisStorage(RedisConfig config) {
        log.info("Using Redis for the cached data");

        this.config = config;
        this.redis = new RedisOperator(config);

        log.info("Using Redis configuration: {}", config);
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue create(String serviceId, KeyValue toCreate) {
        // TODO eviction
        log.info("Creating entry: {}|{}|{}", serviceId, toCreate.getKey(), toCreate.getValue());
        boolean result = redis.create(serviceId, toCreate);
        if (!result) {
            throw new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), toCreate.getKey(), serviceId);
        }
        return toCreate;
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue read(String serviceId, String key) {
        log.info("Reading entry: {}|{}", serviceId, key);
        String result = redis.get(serviceId, key);
        if (result == null) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), key, serviceId);
        }
        return new KeyValue(key, result);
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        log.info("Updating entry: {}|{}|{}", serviceId, toUpdate.getKey(), toUpdate.getValue());
        boolean result = redis.update(serviceId, toUpdate);
        if (!result) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toUpdate.getKey(), serviceId);
        }
        return toUpdate;
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public KeyValue delete(String serviceId, String toDelete) {
        log.info("Deleting entry: {}|{}", serviceId, toDelete);
        String valueToDelete = redis.get(serviceId, toDelete);
        boolean result = redis.delete(serviceId, toDelete);
        if (!result) {
            throw new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), toDelete, serviceId);
        }
        return new KeyValue(toDelete, valueToDelete);
    }

    @Override
    @Retryable(value = RetryableRedisException.class)
    public Map<String, KeyValue> readForService(String serviceId) {
        log.info("Reading all entries: {}", serviceId);
        Map<String, String> redisResult = redis.get(serviceId);
        Map<String, KeyValue> readResult = new HashMap<>();
        for (Map.Entry<String, String> redisEntry : redisResult.entrySet()) {
            readResult.put(redisEntry.getKey(), new KeyValue(redisEntry.getKey(), redisEntry.getValue()));
        }
        return readResult;
    }
}
