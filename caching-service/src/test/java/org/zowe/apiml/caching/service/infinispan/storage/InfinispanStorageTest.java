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

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.lock.api.ClusteredLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.StorageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InfinispanStorageTest {

    public static final KeyValue TO_CREATE = new KeyValue("key1", "val1");
    public static final KeyValue TO_UPDATE = new KeyValue("key1", "val2");
    Cache<String, KeyValue> cache;
    AdvancedCache<String, List<String>> tokenCache;
    InfinispanStorage storage;
    String serviceId1 = "service1";
    ClusteredLock lock;

    @BeforeEach
    void setup() {
        cache = mock(Cache.class);
        tokenCache = mock(AdvancedCache.class);
        storage = new InfinispanStorage(cache, tokenCache, lock);
        lock = mock(ClusteredLock.class);
    }

    @Nested
    class WhenEntryDoesntExist {

        KeyValue keyValue;

        @BeforeEach
        void createEmptyStore() {
            keyValue = null;
        }

        @Test
        void whenRead_thenExceptionIsThrown() {
            String key = TO_CREATE.getKey();
            when(cache.get(serviceId1)).thenReturn(keyValue);
            assertThrows(StorageException.class, () -> storage.read(serviceId1, key));
        }

        @Test
        void whenUpdate_thenExceptionIsThrown() {
            KeyValue entry = new KeyValue("key", "value");
            when(cache.get(serviceId1)).thenReturn(keyValue);
            assertThrows(StorageException.class, () -> storage.update(serviceId1, entry));
        }

        @Test
        void whenAddNew_returnNull() {
            keyValue = new KeyValue("key", "value");
            assertNull(storage.create(serviceId1, keyValue));
        }

        @Test
        void whenDelete_thenExceptionIsThrown() {

            String key = TO_CREATE.getKey();
            when(cache.remove(serviceId1 + key)).thenReturn(null);
            assertThrows(StorageException.class, () -> storage.delete(serviceId1, key));
        }

    }

    @Nested
    class WhenEntryExists {
        KeyValue keyValue;

        @BeforeEach
        void createStoreWithEntry() {
            keyValue = TO_CREATE;
        }

        @Test
        void exceptionIsThrown() {
            when(cache.putIfAbsent(any(), any())).thenReturn(keyValue);
            assertThrows(StorageException.class, () -> storage.create(serviceId1, TO_CREATE));
        }

        @Test
        void entryIsReturned() {
            when(cache.get(serviceId1 + TO_CREATE.getKey())).thenReturn(TO_CREATE);
            KeyValue result = storage.read(serviceId1, TO_CREATE.getKey());
            assertEquals(TO_CREATE.getValue(), result.getValue());
        }

        @Test
        void cacheIsUpdated() {

            when(cache.put(serviceId1 + TO_UPDATE.getKey(), TO_UPDATE)).thenReturn(TO_UPDATE);
            storage.update(serviceId1, TO_UPDATE);
            verify(cache, times(1)).put(serviceId1 + TO_UPDATE.getKey(), TO_UPDATE);
            assertEquals("val2", TO_UPDATE.getValue());
        }

        @Test
        void itemIsDeleted() {
            ConcurrentMap<String, KeyValue> cache = new ConcurrentHashMap<>();
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            assertNull(storage.create(serviceId1, TO_CREATE));
            assertEquals(TO_CREATE, storage.delete(serviceId1, TO_CREATE.getKey()));
        }

        @Test
        void returnAll() {
            ConcurrentMap<String, KeyValue> cache = new ConcurrentHashMap<>();
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            storage.create(serviceId1, new KeyValue("key", "value"));
            storage.create(serviceId1, new KeyValue("key2", "value2"));
            assertEquals(2, storage.readForService(serviceId1).size());
        }

        @Test
        void removeAll() {
            ConcurrentMap<String, KeyValue> cache = new ConcurrentHashMap<>();
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            storage.create(serviceId1, new KeyValue("key", "value"));
            storage.create(serviceId1, new KeyValue("key2", "value2"));
            assertEquals(2, storage.readForService(serviceId1).size());
            storage.deleteForService(serviceId1);
            assertEquals(0, storage.readForService(serviceId1).size());
        }

    }

    @Nested
    class WhenTokenExistsDoesntExist {
        KeyValue keyValue;

        @BeforeEach
        void createEmptyStore() {
            keyValue = null;
        }

        @BeforeEach
        void createStoreWithEntry() {
            when(tokenCache.getAdvancedCache()).thenReturn(tokenCache);
            CompletableFuture<Boolean> cmpl = new CompletableFuture<>();
            cmpl.complete(true);
            when(lock.tryLock(4, TimeUnit.SECONDS)).thenReturn(cmpl);
            when(tokenCache.computeIfAbsent(anyString(), any())).thenAnswer(k -> new ArrayList<>());
        }

        @Test
        void addToken() {

            List<String> list = new ArrayList<>();
            list.add("token");
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            when(tokenCache.computeIfAbsent(anyString(), any(Function.class))).thenAnswer(invocation -> list);
            assertNull(storage.storeInvalidatedToken(serviceId1, new KeyValue("key", "value")));
            verify(tokenCache, times(1)).put(serviceId1 + "key", list);
        }

        @Test
        void throwException() {
            List<String> list = new ArrayList<>();
            list.add("token");
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            when(tokenCache.get(serviceId1 + "key")).thenReturn(Collections.singletonList("token"));
            when(tokenCache.computeIfAbsent(anyString(), any(Function.class))).thenAnswer(invocation -> list);
            assertThrows(StorageException.class, () -> storage.storeInvalidatedToken(serviceId1, new KeyValue("key", "token")));
        }
    }

    @Nested
    class WhenRetrieveToken {

        @Test
        void returnTokenList() {
            List<String> list = new ArrayList<>();
            list.add("token");
            list.add("token2");
            when(tokenCache.get(serviceId1 + "key")).thenReturn(list);
            assertEquals(2, storage.retrieveAllInvalidatedTokens(serviceId1, "key").size());
        }
    }

}
