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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.redis.config.RedisConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RedisStorageTest {
    private static final String SERVICE_ID = "my-service-id";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);
    private static final RedisEntry REDIS_ENTRY = new RedisEntry(SERVICE_ID, KEY_VALUE);

    private RedisStorage underTest;

    private RedisOperator redisOperator;

    @BeforeEach
    void setUp() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy(Strategies.REJECT.getKey());
        generalConfig.setMaxDataSize(100);

        RedisConfig redisConfig = new RedisConfig(generalConfig);
        redisConfig.setHostIP("127.0.0.1");
        redisConfig.setPort(6379);
        redisConfig.setTimeout(60);

        redisOperator = mock(RedisOperator.class);
        underTest = new RedisStorage(redisConfig, redisOperator);
    }

    @Nested
    class whenCreate {
        @Test
        void givenNewKey_thenCreateEntry() throws RedisOutOfMemoryException {
            when(redisOperator.create(any())).thenReturn(true);
            KeyValue result = underTest.create(SERVICE_ID, KEY_VALUE);
            assertThat(result, is(KEY_VALUE));
        }

        @Test
        void givenExistingKey_thenThrowException() throws RedisOutOfMemoryException {
            when(redisOperator.create(any())).thenReturn(false);
            assertThrows(StorageException.class, () -> underTest.create(SERVICE_ID, KEY_VALUE));
        }

        @Test
        void givenRedisOutOfMemory_thenThrowException() throws RedisOutOfMemoryException {
            when(redisOperator.create(any())).thenThrow(new RedisOutOfMemoryException(new Exception()));
            StorageException e = assertThrows(StorageException.class, () -> underTest.create(SERVICE_ID, KEY_VALUE));

            assertThat(e.getKey(), is(Messages.INSUFFICIENT_STORAGE.getKey()));
        }
    }

    @Nested
    class whenRead {
        @Test
        void givenExistingKey_thenReturnKeyPair() {
            when(redisOperator.get(anyString(), anyString())).thenReturn(REDIS_ENTRY);
            KeyValue result = underTest.read(SERVICE_ID, KEY);
            assertThat(result, is(KEY_VALUE));
        }

        @Test
        void givenNotExistingKey_thenThrowException() {
            when(redisOperator.get(anyString(), anyString())).thenReturn(null);
            assertThrows(StorageException.class, () -> underTest.read(SERVICE_ID, KEY));
        }
    }

    @Nested
    class whenUpdate {
        @Test
        void givenExistingKey_thenUpdateKeyPair() throws RedisOutOfMemoryException {
            when(redisOperator.update(any())).thenReturn(true);
            KeyValue result = underTest.update(SERVICE_ID, KEY_VALUE);
            assertThat(result, is(KEY_VALUE));
        }

        @Test
        void givenNewKey_thenThrowException() throws RedisOutOfMemoryException {
            when(redisOperator.update(any())).thenReturn(false);
            assertThrows(StorageException.class, () -> underTest.update(SERVICE_ID, KEY_VALUE));
        }

        @Test
        void givenRedisOutOfMemory_thenThrowException() throws RedisOutOfMemoryException {
            when(redisOperator.update(any())).thenThrow(new RedisOutOfMemoryException(new Exception()));
            StorageException e = assertThrows(StorageException.class, () -> underTest.update(SERVICE_ID, KEY_VALUE));

            assertThat(e.getKey(), is(Messages.INSUFFICIENT_STORAGE.getKey()));
        }
    }

    @Nested
    class whenDelete {
        @Test
        void givenExistingKey_thenRemoveKey() {
            when(redisOperator.get(anyString(), anyString())).thenReturn(REDIS_ENTRY);
            when(redisOperator.delete(anyString(), anyString())).thenReturn(true);
            KeyValue result = underTest.delete(SERVICE_ID, KEY);
            assertThat(result, is(KEY_VALUE));
        }

        @Test
        void givenNewKey_thenThrowException() {
            when(redisOperator.delete(anyString(), any())).thenReturn(false);
            assertThrows(StorageException.class, () -> underTest.delete(SERVICE_ID, KEY));
        }
    }

    @Nested
    class whenReadForService {
        @Test
        void givenServiceWithEntries_thenReturnEntries() {
            Map<String, KeyValue> expected = new HashMap<>();
            expected.put(KEY, KEY_VALUE);

            List<RedisEntry> redisResults = new ArrayList<>();
            redisResults.add(REDIS_ENTRY);
            when(redisOperator.get(anyString())).thenReturn(redisResults);

            Map<String, KeyValue> result = underTest.readForService(SERVICE_ID);
            assertThat(result, is(expected));
        }

        @Test
        void givenServiceWithNoEntries_thenReturnEmptyMap() {
            when(redisOperator.get(anyString())).thenReturn(new ArrayList<>());

            Map<String, KeyValue> result = underTest.readForService(SERVICE_ID);
            assertThat(result.size(), is(0));
        }
    }
}
