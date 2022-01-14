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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InfinispanStorageTest {

    public static final KeyValue TO_CREATE = new KeyValue("key1", "val1");
    public static final KeyValue TO_UPDATE = new KeyValue("key1", "val2");
    Cache<String, Map<String, KeyValue>> cache;
    InfinispanStorage storage;
    String serviceId1 = "service1";

    @BeforeEach
    void setup() {
        cache = mock(Cache.class);
        storage = new InfinispanStorage(cache);
    }

    @Nested
    class WhenEntryDoesntExist {

        Map<String, KeyValue> serviceStore;

        @BeforeEach
        void createEmptyStore() {
            serviceStore = new HashMap<>();
        }

        @Test
        void whenCreate_thenCacheIsUpdated() {
            when(cache.computeIfAbsent(any(), any())).thenReturn(serviceStore);
            storage.create(serviceId1, TO_CREATE);
            verify(cache, times(1)).put(serviceId1, serviceStore);
        }

        @Test
        void whenRead_thenExceptionIsThrown() {
            String key = TO_CREATE.getKey();
            when(cache.get(serviceId1)).thenReturn(serviceStore);
            assertThrows(StorageException.class, () -> storage.read(serviceId1, key));
        }

        @Test
        void whenDelete_thenExceptionIsThrown() {

            String key = TO_CREATE.getKey();
            when(cache.get(serviceId1)).thenReturn(serviceStore);
            assertThrows(StorageException.class, () -> storage.delete(serviceId1, key));
            verify(cache, times(0)).put(serviceId1, serviceStore);
        }

        @Test
        void whenUpdate_thenCacheIsUpdated() {

            when(cache.get(serviceId1)).thenReturn(serviceStore);
            assertThrows(StorageException.class, () -> storage.update(serviceId1, TO_UPDATE));
            verify(cache, times(0)).put(serviceId1, serviceStore);
        }
    }


    @Nested
    class WhenEntryExists {
        Map<String, KeyValue> serviceStore;

        @BeforeEach
        void createStoreWithEntry() {
            serviceStore = new HashMap<>();
            serviceStore.put(TO_CREATE.getKey(), TO_CREATE);
        }

        @Test
        void whenCreate_thenExceptionIsThrown() {
            when(cache.computeIfAbsent(any(), any())).thenReturn(serviceStore);
            assertThrows(StorageException.class, () -> storage.create(serviceId1, TO_CREATE));
        }

        @Test
        void whenRead_thenEntryIsReturned() {
            when(cache.get(serviceId1)).thenReturn(serviceStore);
            KeyValue result = storage.read(serviceId1, TO_CREATE.getKey());
            assertEquals(TO_CREATE.getValue(), result.getValue());
        }

        @Test
        void whenUpdate_thenCacheIsUpdated() {

            when(cache.get(serviceId1)).thenReturn(serviceStore);
            storage.update(serviceId1, TO_UPDATE);
            verify(cache, times(1)).put(serviceId1, serviceStore);
            assertEquals("val2", serviceStore.get(TO_CREATE.getKey()).getValue());
        }

        @Test
        void whenDelete_thenCacheIsUpdated() {

            when(cache.get(serviceId1)).thenReturn(serviceStore);
            KeyValue result = storage.delete(serviceId1, TO_CREATE.getKey());
            verify(cache, times(1)).put(serviceId1, serviceStore);
            assertEquals(TO_CREATE.getValue(), result.getValue());
            assertNull(serviceStore.get(TO_CREATE.getKey()));
        }

    }


}
