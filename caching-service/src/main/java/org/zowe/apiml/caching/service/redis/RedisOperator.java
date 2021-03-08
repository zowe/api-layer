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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.redis.config.RedisConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RedisOperator {
    private final RedisAsyncCommands<String, String> redis;

    public RedisOperator(RedisConfig config) {
        // TODO how to handle authentication to redis?
        RedisURI redisUri = new RedisURI(config.getHostIP(), config.getPort(), Duration.ofSeconds(config.getTimeout()));
        RedisClient redisClient = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        redis = connection.async();
        // TODO should release redis connection on caching service end via connection.close(); client.shutdown();
        // this could be via closeable and try with resources, but then re-connecting to redis every time try to CRUD
        // would be better to keep connection open until RedisOperator is destructed
    }

    public boolean create(String serviceId, KeyValue toAdd) {
        try {
            RedisFuture<Boolean> result = redis.hsetnx(serviceId, toAdd.getKey(), toAdd.toString());
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    public boolean update(String serviceId, KeyValue toUpdate) {
        try {
            RedisFuture<Boolean> exists = redis.hexists(serviceId, toUpdate.getKey());
            if (!exists.get()) {
                return false;
            }

            RedisFuture<Boolean> result = redis.hset(serviceId, toUpdate.getKey(), toUpdate.getValue());
            return !result.get(); // hset returns false if field already exists and value was updated
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    public String get(String serviceId, String key) {
        try {
            RedisFuture<String> result = redis.hget(serviceId, key);
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    public Map<String, String> get(String serviceId) {
        try {
            RedisFuture<Map<String, String>> result = redis.hgetall(serviceId);
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    public boolean delete(String serviceId, String toDelete) {
        try {
            RedisFuture<Long> result = redis.hdel(serviceId, toDelete);
            long recordsDelete = result.get();
            return recordsDelete >= 1;
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }
}
