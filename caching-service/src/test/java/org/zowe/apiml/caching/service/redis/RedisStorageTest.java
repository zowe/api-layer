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
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.redis.exceptions.RedisOutOfMemoryException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
        redisOperator = mock(RedisOperator.class);
        underTest = new RedisStorage(redisOperator);
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
            StorageException e = assertThrows(StorageException.class, () -> underTest.create(SERVICE_ID, KEY_VALUE));

            assertThat(e.getKey(), is(Messages.DUPLICATE_KEY.getKey()));
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
            StorageException e = assertThrows(StorageException.class, () -> underTest.read(SERVICE_ID, KEY));

            assertThat(e.getKey(), is(Messages.KEY_NOT_IN_CACHE.getKey()));
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
            StorageException e = assertThrows(StorageException.class, () -> underTest.update(SERVICE_ID, KEY_VALUE));

            assertThat(e.getKey(), is(Messages.KEY_NOT_IN_CACHE.getKey()));
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
            StorageException e = assertThrows(StorageException.class, () -> underTest.delete(SERVICE_ID, KEY));

            assertThat(e.getKey(), is(Messages.KEY_NOT_IN_CACHE.getKey()));
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

    @Nested
    class whenDeleteForService {
        @Test
        void givenServiceId_thenCallDeleteWithServiceId() {
            when(redisOperator.delete(SERVICE_ID)).thenReturn(true);
            underTest.deleteForService(SERVICE_ID);
            verify(redisOperator, times(1)).delete(SERVICE_ID);
        }

        @Test
        void givenFailureToDelete_thenDontThrowException() {
            when(redisOperator.delete(any())).thenReturn(false);
            assertDoesNotThrow(() -> underTest.deleteForService(SERVICE_ID));
        }
    }

    @Nested
    class WhenTryingToStoreToken {
        @Test
        void thenThrowException() {
            KeyValue keyValue = new KeyValue("key", "value");
            assertThrows(StorageException.class, () -> {
                underTest.storeMapItem(SERVICE_ID, "mapKey", keyValue);
            });
        }
    }

    @Nested
    class WhenTryingToGetTokens {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.getAllMapItems(SERVICE_ID, "key");
            });
        }
    }

    @Nested
    class WhenTryingToGetAllMaps {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.getAllMaps(SERVICE_ID);
            });
        }
    }

    @Nested
    class WhenTryingToDeleteTokens {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.removeNonRelevantTokens(SERVICE_ID, "key");
            });
        }
    }

    @Nested
    class WhenTryingToDeleteRules {
        @Test
        void thenThrowException() {
            assertThrows(StorageException.class, () -> {
                underTest.removeNonRelevantRules(SERVICE_ID, "key");
            });
        }
    }
}
