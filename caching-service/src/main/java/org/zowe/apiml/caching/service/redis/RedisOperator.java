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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class RedisOperator {
    private final RedisAsyncCommands<String, String> redis;

    public RedisOperator(String host, int port, Duration timeout) {
        // TODO how to handle authentication to redis?
        RedisURI redisUri = new RedisURI(host, port, timeout);
        RedisClient redisClient = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        redis = connection.async();
        // TODO should release redis connection on caching service end via connection.close(); client.shutdown();
        // this could be via closeable and try with resources, but then re-connecting to redis every time try to CRUD
        // would be better to keep connection open until RedisOperator is destructed
    }

    public boolean set(String serviceId, KeyValue toSet) throws ExecutionException, InterruptedException {
        // TODO if key exists, this will return false - good for create, but unclear how to update key then
        RedisFuture<Boolean> result = redis.hset(serviceId, toSet.getKey(), toSet.getValue());
        return result.get();
    }

    public String get(String serviceId, String key) throws ExecutionException, InterruptedException {
        RedisFuture<String> result = redis.hget(serviceId, key);
        return result.get();
    }

    public Map<String, String> get(String serviceId) throws ExecutionException, InterruptedException {
        RedisFuture<Map<String, String>> result = redis.hgetall(serviceId);
        return result.get();
    }

    public boolean delete(String serviceId, String toDelete) throws ExecutionException, InterruptedException {
        RedisFuture<Long> result = redis.hdel(serviceId, toDelete);
        long recordsDelete = result.get();
        return recordsDelete >= 1; // TODO is it really successful if deleted more than 1 records? Expect just 1 deleted at a time.
    }
}
