/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;
import org.zowe.apiml.cache.CompositeKey;

import javax.cache.Cache.Entry;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheUtilsTest {

    private CacheUtils underTest;

    @BeforeEach
    void setUp() {
        underTest = new CacheUtils();
    }

    private javax.cache.Cache.Entry<Object, Object> createEntry(Object key, Object value) {
        return new javax.cache.Cache.Entry<Object, Object>() {
            @Override
            public Object getKey() {
                return key;
            }

            @Override
            public Object getValue() {
                return value;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T unwrap(Class<T> clazz) {
                return (T) value;
            }
        };
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testEvictSubset() {
        CacheManager cacheManager = mock(CacheManager.class);

        // cache1 is not ehCache
        Cache cache1 = mock(Cache.class);
        when(cacheManager.getCache("cache1")).thenReturn(cache1);
        when(cache1.getNativeCache()).thenReturn(Object.class);

        Cache cache2 = mock(Cache.class);
        when(cacheManager.getCache("cache2")).thenReturn(cache2);
        javax.cache.Cache ehCache2 = mock(javax.cache.Cache.class);

        when(cache2.getNativeCache()).thenReturn(ehCache2);
        List<Object> keys = Arrays.asList(
            "abc", // not composite key
            new CompositeKey("test", 5),
            new CompositeKey("next", 10),
            new CompositeKey("next", 15)
        );
        List<javax.cache.Cache.Entry<Object, Object>> values = Arrays.asList(
                createEntry(keys.get(0), "A"),
                createEntry(keys.get(1), "B"),
                createEntry(keys.get(2), "C"),
                createEntry(keys.get(3), "D")
        );

        when(ehCache2.spliterator()).thenAnswer(invocation -> values.spliterator());

        try {
            underTest.evictSubset(cacheManager, "missing", x -> true);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unknown cache"));
            assertTrue(e.getMessage().contains("missing"));
        }

        // not EhCache - clean all, do not use keyPredicate
        verify(cache1, never()).clear();
        underTest.evictSubset(cacheManager, "cache1", x -> false);
        verify(cache1, times(1)).clear();

        // in all cases remove entries without CompositeKey
        underTest.evictSubset(cacheManager, "cache2", x -> false);
        verify(ehCache2, times(1)).removeAll(Collections.singleton(keys.get(0)));

        underTest.evictSubset(cacheManager, "cache2", x -> x.equals(0, "test"));
        verify(ehCache2, times(1)).removeAll(new HashSet(Arrays.asList(keys.get(0), keys.get(1))));

        underTest.evictSubset(cacheManager, "cache2", x -> (Integer) x.get(1) > 10);
        verify(ehCache2, times(1)).removeAll(new HashSet(Arrays.asList(keys.get(0), keys.get(3))));
    }

    @Test
    void givenUnknownCacheName_whenGetAllRecords_thenThrowsException() {
        CacheManager cacheManager = mock(CacheManager.class);
        IllegalArgumentException iae = assertThrows(
            IllegalArgumentException.class,
            () -> underTest.getAllRecords(cacheManager, "unknownCacheName")
        );
        assertEquals("Unknown cache unknownCacheName", iae.getMessage());
    }

    @Test
    void givenNoOpCache_whenGetAllRecords_thenEmpty() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("knownCacheName")).thenReturn(cache);
        when(cache.getNativeCache()).thenReturn(new NoOpCache("knownCacheName"));

        assertEquals(0, underTest.getAllRecords(cacheManager, "knownCacheName").size());
    }

    @Test
    void givenUnsupportedCacheManager_whenGetAllRecords_thenThrowsException() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("knownCacheName")).thenReturn(cache);
        when(cache.getNativeCache()).thenReturn(new Object());
        IllegalArgumentException iae = assertThrows(
            IllegalArgumentException.class,
            () -> underTest.getAllRecords(cacheManager, "knownCacheName")
        );
        assertTrue(iae.getMessage().startsWith("Unsupported type of cache : "));
    }

    @Test
    void givenValidCacheManager_whenGetAllRecords_thenReadAllStoredRecords() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        javax.cache.Cache<?, ?> ehCache = mock(javax.cache.Cache.class);

        List<Entry<Object, Object>> entries = Arrays.asList(
            createEntry(1, "a"),
            createEntry(2, "b"),
            createEntry(3, "c")
        );

        when(cacheManager.getCache("knownCacheName")).thenReturn(cache);
        when(cache.getNativeCache()).thenReturn(ehCache);
        when(ehCache.spliterator()).thenAnswer(invocation -> entries.spliterator());

        Collection<String> values = underTest.getAllRecords(cacheManager, "knownCacheName");
        assertNotNull(values);
        assertEquals(3, values.size());
        assertTrue(values.contains("a"));
        assertTrue(values.contains("b"));
        assertTrue(values.contains("c"));
    }

}
