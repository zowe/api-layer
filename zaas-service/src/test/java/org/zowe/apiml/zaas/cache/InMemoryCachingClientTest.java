/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.caching.model.KeyValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InMemoryCachingClientTest {

    private Storage storage;
    private LocalCachingClient cachingClient;

    /**
     * Subclass that overrides getServiceId() to avoid certificate initialization.
     */
    static class TestLocalCachingClient extends LocalCachingClient {
        TestLocalCachingClient(Storage storage) {
            super(storage);
        }

        @Override
        String getServiceId() {
            return "CN=TestService";
        }
    }

    @BeforeEach
    void setUp() {
        storage = mock(Storage.class);
        cachingClient = new TestLocalCachingClient(storage);
    }

    @Test
    void testCreate() {
        var kv = new CachingServiceClient.KeyValue("key1", "value1");

        cachingClient.create(kv);

        verify(storage).create(eq("CN=TestService"), argThat(kv1 ->
            kv1.getKey().equals("key1") && kv1.getValue().equals("value1")));
    }

    @Test
    void testUpdate() {
        var kv = new CachingServiceClient.KeyValue("key2", "value2");

        cachingClient.update(kv);

        verify(storage).update(eq("CN=TestService"), argThat(kv1 ->
            kv1.getKey().equals("key2") && kv1.getValue().equals("value2")));
    }

    @Test
    void testDelete() {
        cachingClient.delete("toDelete");

        verify(storage).delete("CN=TestService", "toDelete");
    }

    @Test
    void testRead() {
        when(storage.read("CN=TestService", "keyX"))
            .thenReturn(new KeyValue("keyX", "valueX"));

        var result = cachingClient.read("keyX");

        assertNotNull(result);
        assertEquals("keyX", result.getKey());
        assertEquals("valueX", result.getValue());
    }

    @Test
    void testAppendList() {
        var kv = new CachingServiceClient.KeyValue("listKey", "item1");

        cachingClient.appendList("listMap", kv);

        verify(storage).storeMapItem(eq("CN=TestService"), eq("listMap"), argThat(kv1 ->
            kv1.getKey().equals("listKey") && kv1.getValue().equals("item1")));
    }

    @Test
    void testReadAllMaps() {
        Map<String, Map<String, String>> mockData = Map.of("map1", Map.of("k1", "v1"));
        when(storage.getAllMaps("CN=TestService")).thenReturn(mockData);

        var result = cachingClient.readAllMaps();

        assertEquals(mockData, result);
    }

    @Test
    void testEvictTokens() {
        cachingClient.evictTokens("userToken");

        verify(storage).removeNonRelevantTokens("CN=TestService", "userToken");
    }

    @Test
    void testEvictRules() {
        cachingClient.evictRules("ruleKey");

        verify(storage).removeNonRelevantRules("CN=TestService", "ruleKey");
    }
}
