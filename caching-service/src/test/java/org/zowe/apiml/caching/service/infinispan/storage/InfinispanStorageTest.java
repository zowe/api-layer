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

import org.infinispan.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.StorageException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InfinispanStorageTest {

    public static final KeyValue TO_CREATE = new KeyValue("key1", "val1");
    public static final KeyValue TO_UPDATE = new KeyValue("key1", "val2");
    Cache<String, KeyValue> cache;
    InfinispanStorage storage;
    String serviceId1 = "service1";

    @BeforeEach
    void setup() {
        cache = mock(Cache.class);
        storage = new InfinispanStorage(cache);
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
            InfinispanStorage storage = new InfinispanStorage(cache);
            assertNull(storage.create(serviceId1, TO_CREATE));
            assertEquals(TO_CREATE, storage.delete(serviceId1, TO_CREATE.getKey()));
        }

        @Test
        void returnAll() {
            ConcurrentMap<String, KeyValue> cache = new ConcurrentHashMap<>();
            InfinispanStorage storage = new InfinispanStorage(cache);
            storage.create(serviceId1, new KeyValue("key", "value"));
            storage.create(serviceId1, new KeyValue("key2", "value2"));
            assertEquals(2, storage.readForService(serviceId1).size());
        }

        @Test
        void removeAll() {
            ConcurrentMap<String, KeyValue> cache = new ConcurrentHashMap<>();
            InfinispanStorage storage = new InfinispanStorage(cache);
            storage.create(serviceId1, new KeyValue("key", "value"));
            storage.create(serviceId1, new KeyValue("key2", "value2"));
            assertEquals(2, storage.readForService(serviceId1).size());
            storage.deleteForService(serviceId1);
            assertEquals(0, storage.readForService(serviceId1).size());
        }

    }


}
