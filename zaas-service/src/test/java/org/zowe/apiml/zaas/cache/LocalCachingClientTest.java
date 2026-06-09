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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.security.HttpsConfig;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalCachingClientTest {
    private static final String SERVICE_ID = "CN=test-service";

    private Storage mockStorage;
    private HttpsConfig mockHttpsConfig;
    private LocalCachingClient underTest;

    @BeforeEach
    void setUp() {
        mockStorage = mock(Storage.class);
        mockHttpsConfig = mock(HttpsConfig.class);
        X509Certificate mockCert = mock(X509Certificate.class);
        X500Principal principal = new X500Principal(SERVICE_ID);
        when(mockCert.getSubjectX500Principal()).thenReturn(principal);
        when(mockHttpsConfig.getCertificate()).thenReturn(mockCert);
        underTest = new LocalCachingClient(mockStorage, mockHttpsConfig);
    }

    @Nested
    class GivenReadMap {
        @Test
        void whenStorageReturnsMap_thenReturnMap() {
            String mapKey = "testMap";
            Map<String, String> expected = new HashMap<>();
            expected.put("key1", "value1");
            expected.put("key2", "value2");
            when(mockStorage.getAllMapItems(SERVICE_ID, mapKey)).thenReturn(expected);

            Map<String, String> result = underTest.readMap(mapKey);

            assertThat(result.size(), is(2));
            assertThat(result.get("key1"), is("value1"));
            assertThat(result.get("key2"), is("value2"));
            verify(mockStorage).getAllMapItems(SERVICE_ID, mapKey);
        }

        @Test
        void whenStorageReturnsNull_thenReturnNull() {
            String mapKey = "testMap";
            when(mockStorage.getAllMapItems(SERVICE_ID, mapKey)).thenReturn(null);

            Map<String, String> result = underTest.readMap(mapKey);

            assertNull(result);
            verify(mockStorage).getAllMapItems(SERVICE_ID, mapKey);
        }

        @Test
        void whenStorageReturnsEmptyMap_thenReturnEmptyMap() {
            String mapKey = "testMap";
            when(mockStorage.getAllMapItems(SERVICE_ID, mapKey)).thenReturn(new HashMap<>());

            Map<String, String> result = underTest.readMap(mapKey);

            assertTrue(result.isEmpty());
            verify(mockStorage).getAllMapItems(SERVICE_ID, mapKey);
        }
    }

    @Nested
    class GivenDeleteMapItem {
        @Test
        void deleteMapItemDelegatesToStorage() {
            String mapKey = "testMap";
            String entryKey = "testEntry";

            underTest.deleteMapItem(mapKey, entryKey);

            verify(mockStorage).deleteMapItem(SERVICE_ID, mapKey, entryKey);
        }
    }
}
