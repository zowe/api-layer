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

import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.message.log.ApimlLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Class used to connect to and operate on a Redis instance or cluster.
 * Contains the CRUD operations enacted on Redis with serialized read and write.
 */
@AllArgsConstructor
@Slf4j
public class RedisOperator {
    private RedisAsyncCommands<String, String> redis;

    public RedisOperator(RedisURI redisUri, ApimlLogger apimlLog) {
        try {
            RedisClient redisClient = RedisClient.create();
            StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(redisClient, StringCodec.UTF8, redisUri);
            redis = connection.async();
        } catch (RedisConnectionException e) {
            apimlLog.log("org.zowe.apiml.cache.errorInitializingStorage", "redis", e.getCause().getMessage(), e);
            System.exit(1);
        }
        // TODO should release redis connection on caching service end via connection.close(); client.shutdown();
        // this could be via closeable and try with resources, but then re-connecting to redis every time try to CRUD
        // would be better to keep connection open until RedisOperator is destructed
    }

    /**
     * Creates a given entry in Redis.
     *
     * @param entryToAdd RedisEntry containing the service ID for which to create the entry, and the key and value.
     * @return true if the key does not exist for the service ID and the entry was created, otherwise false.
     */
    public boolean create(RedisEntry entryToAdd) throws RedisOutOfMemoryException {
        KeyValue toAdd = entryToAdd.getEntry();

        try {
            RedisFuture<Boolean> result = redis.hsetnx(entryToAdd.getServiceId(), toAdd.getKey(), entryToAdd.getEntryAsString());
            return result.get();
        } catch (ExecutionException e) {
            handleWriteOperationExecutionException(e);
        } catch (InterruptedException e) {
            throw new RetryableRedisException(e);
        } catch (RedisEntryException e) {
            return false;
        }

        return false;
    }

    /**
     * Updates a given entry in Redis.
     *
     * @param entryToUpdate RedisEntry containing the service ID and key to update, with the new value.
     * @return true if the key exists for a service ID and the value was updated, otherwise false.
     */
    public boolean update(RedisEntry entryToUpdate) throws RedisOutOfMemoryException {
        String serviceId = entryToUpdate.getServiceId();
        KeyValue toUpdate = entryToUpdate.getEntry();

        try {
            boolean exists = redis.hexists(serviceId, toUpdate.getKey()).get();
            if (!exists) {
                return false;
            }

            boolean result = redis.hset(serviceId, toUpdate.getKey(), entryToUpdate.getEntryAsString()).get();
            return !result; // hset returns false if field already exists and value was updated
        } catch (ExecutionException e) {
            handleWriteOperationExecutionException(e);
        } catch (InterruptedException e) {
            throw new RetryableRedisException(e);
        } catch (RedisEntryException e) {
            return false;
        }

        return false;
    }

    /**
     * Retrieve an entry for a given service with the corresponding key.
     *
     * @return RedisEntry instance if the service ID and key exist, otherwise null.
     */
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

    /**
     * Retrieves all entries for a given service.
     *
     * @return List of RedisEntry instances. If there are no entries an empty List is returned.
     */
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

    /**
     * Deletes all entries with the given key for a given service.
     *
     * @return true if at least one entry was deleted, otherwise false.
     */
    public boolean delete(String serviceId, String toDelete) {
        try {
            long recordsDeleted = redis.hdel(serviceId, toDelete).get();
            return recordsDeleted >= 1;
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    /**
     * Deletes all entries for a given service.
     *
     * @return true if at least one entry was deleted, otherwise false.
     */
    public boolean delete(String serviceId) {
        try {
            long recordsDeleted = redis.del(serviceId).get();
            return recordsDeleted >= 1;
        } catch (InterruptedException | ExecutionException e) {
            throw new RetryableRedisException(e);
        }
    }

    private void handleWriteOperationExecutionException(ExecutionException e) throws RedisOutOfMemoryException {
        Throwable cause = e.getCause();
        if (cause instanceof RedisCommandExecutionException && cause.getMessage().contains("maxmemory")) {
            throw new RedisOutOfMemoryException(cause);
        } else {
            throw new RetryableRedisException(e);
        }
    }
}
