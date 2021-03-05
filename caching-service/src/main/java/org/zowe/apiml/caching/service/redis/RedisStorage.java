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
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.redis.config.RedisConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

// TODO abstract out lettuce elements to one file, not here
// TODO hset fails if key already exists - good for create, bad for update
@Slf4j
public class RedisStorage implements Storage {
    private RedisConfig config;
    private RedisAsyncCommands<String, String> redis;

    public RedisStorage(RedisConfig config, RedisURI redisUri) {
        this.config = config;

        // TODO should release redis connection on caching service end via connection.close(); client.shutdown();
        RedisClient redisClient = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        redis = connection.async();
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        // TODO check if key already exists
        RedisFuture<Boolean> result = redis.hset(serviceId, toCreate.getKey(), toCreate.getValue());
        try {
            Boolean boolResult = result.get();
            // TODO check if result ok, if not need details to know what exception to throw
            return toCreate;
        } catch (ExecutionException | InterruptedException e) {
            // TODO throw a storage exception
            return null;
        }
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        RedisFuture<String> result = redis.hget(serviceId, key);
        try {
            String a = result.get();
            return new KeyValue(key, a);
        } catch (ExecutionException | InterruptedException e) {
            // TODO throw a storage exception
            return null;
        }
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        RedisFuture<Boolean> result = redis.hset(serviceId, toUpdate.getKey(), toUpdate.getValue());
        try {
            Boolean boolResult = result.get();
            // TODO check if result ok, if not need details to know what exception to throw
            return toUpdate;
        } catch (ExecutionException | InterruptedException e) {
            // TODO throw a storage exception
            return null;
        }
    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        // TODO check if key already exists
        RedisFuture<Long> result = redis.hdel(serviceId, toDelete);
        try {
            Long longResult = result.get();
            // TODO check if result ok
            return new KeyValue(toDelete, toDelete); // TODO what is the return here
        } catch (ExecutionException | InterruptedException e) {
            // TODO storage exception
            return null;
        }
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        RedisFuture<Map<String, String>> result = redis.hgetall(serviceId);
        try {
            Map<String, String> mapResult = result.get();
            // TODO check if result ok
            Map<String, KeyValue> r = new HashMap<>();
            r.put(serviceId, new KeyValue("key", "value"));
            return r; // TODO what is return here
        } catch (ExecutionException | InterruptedException e) {
            // TODO storage exception
            return null;
        }
    }
}
