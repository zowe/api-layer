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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InfinispanStorageTest {

    public static final KeyValue TO_CREATE = new KeyValue("key1", "val1");
    public static final KeyValue TO_UPDATE = new KeyValue("key1", "val2");
    Cache<String, KeyValue> cache;
    AdvancedCache<String, Map<String,String>> tokenCache;
    InfinispanStorage storage;
    String serviceId1 = "service1";

    String serviceId2 = "service2";
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
    class WhenStoreToken {
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
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("key", "token");
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            when(tokenCache.get(anyString())).thenAnswer(invocation -> hashMap);
            assertNull(storage.storeMapItem(serviceId1, "invalidTokens", new KeyValue("newkey", "newvalue")));
            verify(tokenCache, times(1)).put(serviceId1 + "invalidTokens", hashMap);
        }

        @Test
        void updateToken() {
            HashMap<String, String> hashMap = new HashMap();
            hashMap.put("key", "token");
            InfinispanStorage storage = new InfinispanStorage(cache, tokenCache, lock);
            when(tokenCache.get(serviceId1 + "invalidTokens")).thenReturn(hashMap);
            KeyValue keyValue = new KeyValue("key", "token2");
            assertNull(storage.storeMapItem(serviceId1, "invalidTokens", keyValue));
            verify(tokenCache, times(1)).put(serviceId1 + "invalidTokens", hashMap);
        }
    }

    @Nested
    class WhenRetrieveToken {

        @Test
        void returnTokenList() {
            HashMap<String, String> expectedMap = new HashMap();
            expectedMap.put("key1", "token1");
            expectedMap.put("key2", "token2");

            when(tokenCache.get(serviceId1 + "invalidTokens")).thenReturn(expectedMap);
            assertEquals(2, storage.getAllMapItems(serviceId1, "invalidTokens").size());
        }
    }

    @Nested
    class WhenRetrieveInvalidTokensAndRules {

        InfinispanStorage underTest;
        @BeforeEach
        void createStorage() {
            Map<String, String> tokensService1 = new HashMap();
            tokensService1.put("key1", "token1");
            tokensService1.put("key2", "token2");
            Map<String, String> tokensService2 = new HashMap();
            tokensService2.put("key3", "token3");
            Map<String, String> rulesService1 = new HashMap();
            rulesService1.put("key1", "rule1");
            rulesService1.put("key2", "rule2");
            ConcurrentMap<String, Map<String, String>> tokenCache = new ConcurrentHashMap<>();
            tokenCache.put(serviceId1 + "invalidTokens", tokensService1);
            tokenCache.put(serviceId1 + "invalidTokenRules", rulesService1);
            tokenCache.put(serviceId2 + "invalidTokens", tokensService2);
            underTest = new InfinispanStorage(cache, tokenCache, lock);
        }


        @Test
        void returnAllForGivenService() {
            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId1);

            assertEquals(2, result.size());
            assertNotNull(result.get("invalidTokens"));
            assertEquals(2, result.get("invalidTokens").size());
            assertNotNull(result.get("invalidTokenRules"));
            assertEquals(2, result.get("invalidTokenRules").size());
        }

        @Test
        void returnAllForAnotherService() {
            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId2);

            assertEquals(1, result.size());
            assertNotNull(result.get("invalidTokens"));
            assertEquals(1, result.get("invalidTokens").size());
            assertNull(result.get("invalidTokenRules"));
        }

        @Test
        void returnNoneForUnknownService() {
            Map<String, Map<String, String>> result = underTest.getAllMaps("unknown_service");

            assertEquals(0, result.size());

        }
    }

    @Nested
    class WhenEvictNonRelevantTokensAndRules {
        InfinispanStorage underTest;

        @BeforeEach
        void createStorage() {
            Map<String, String> tokensService = new HashMap();
            String value = "{\"userId\":null,\"tokenValue\":\"hashedKey\",\"issuedAt\":[2022,8,17,16,13,18],\"expiresAt\":[2021,11,15,15,13,18],\"scopes\":null,\"tokenProvider\":null}";
            tokensService.put("key1", value);
            tokensService.put("key2", "token");
            Map<String, String> rulesService = new HashMap();
            rulesService.put("key1", "1595282400000");
            Map<String, String> rulesUsers = new HashMap();
            rulesUsers.put("key1", "1595282400000");
            ConcurrentMap<String, Map<String, String>> tokenCache = new ConcurrentHashMap<>();
            tokenCache.put(serviceId1 + "invalidTokens", tokensService);
            tokenCache.put(serviceId1 + "invalidScopes", rulesService);
            tokenCache.put(serviceId1 + "invalidUsers", rulesUsers);
            underTest = new InfinispanStorage(cache, tokenCache, lock);
        }
        @Test
        void thenEvictItems() {
            CompletableFuture<Boolean> cmpl = new CompletableFuture<>();
            cmpl.complete(true);
            when(lock.tryLock(4, TimeUnit.SECONDS)).thenReturn(cmpl);
            underTest.removeNonRelevantTokens(serviceId1, "invalidTokens");
            underTest.removeNonRelevantRules(serviceId1, "invalidScopes");
            underTest.removeNonRelevantRules(serviceId1, "invalidUsers");
            Map<String, Map<String, String>> result = underTest.getAllMaps(serviceId1);
            assertEquals(1, result.get("invalidTokens").size());
            assertEquals(0, result.get("invalidScopes").size());
            assertEquals(0, result.get("invalidUsers").size());
        }

    }

}
