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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.redis.config.RedisConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Slf4j
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

    public boolean create(RedisEntry entryToAdd) {
        try {
            KeyValue toAdd = entryToAdd.getEntry();
            RedisFuture<Boolean> result = redis.hsetnx(entryToAdd.getServiceId(), toAdd.getKey(), entryToAdd.getEntryAsString());
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        } catch (RedisEntryException e) {
            return false;
        }
    }

    public boolean update(RedisEntry entryToUpdate) {
        try {
            String serviceId = entryToUpdate.getServiceId();
            KeyValue toUpdate = entryToUpdate.getEntry();

            boolean exists = redis.hexists(serviceId, toUpdate.getKey()).get();
            if (!exists) {
                return false;
            }

            boolean result = redis.hset(serviceId, toUpdate.getKey(), entryToUpdate.getEntryAsString()).get();
            return !result; // hset returns false if field already exists and value was updated
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        } catch (RedisEntryException e) {
            return false;
        }
    }

    public RedisEntry get(String serviceId, String key) {
        try {
            String result = redis.hget(serviceId, key).get();
            return new RedisEntry(serviceId, result);
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        } catch (RedisEntryException e) {
            log.warn("Error retrieving entry: {}|{}. Error: {}", serviceId, key, e.getMessage());
            return null;
        }
    }

    public List<RedisEntry> get(String serviceId) {
        try {
            Map<String, String> result = redis.hgetall(serviceId).get();
            List<RedisEntry> entries = new ArrayList<>();

            for (Map.Entry<String, String> entry : result.entrySet()) {
                try {
                    entries.add(new RedisEntry(serviceId, entry.getValue()));
                } catch (RedisEntryException e) {
                    log.warn("Error retrieving entry: {}|{}. Error: {}", serviceId, entry, e.getMessage());
                }
            }

            return entries;
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    public boolean delete(String serviceId, String toDelete) {
        try {
            long recordsDeleted = redis.hdel(serviceId, toDelete).get();
            return recordsDeleted >= 1;
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }
}
