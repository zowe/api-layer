/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.inmemory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.caching.model.KeyValue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class RemoveOldestStrategyTest {
    private RemoveOldestStrategy underTest;
    private Map<String, KeyValue> dataForStorage;

    @BeforeEach
    void setUp() {
        Map<String, Map<String, KeyValue>> storage = new ConcurrentHashMap<>();
        dataForStorage = new ConcurrentHashMap<>();
        storage.put("test-service", dataForStorage);
        KeyValue keyValue1 = new KeyValue("key1", "willBeRemoved", "1610965944035");
        keyValue1.setServiceId("test-service");

        KeyValue keyValue2 = new KeyValue("key2", "willFit", "1610965944036");
        keyValue2.setServiceId("test-service");

        dataForStorage.put("key1", keyValue1);
        dataForStorage.put("key2", keyValue2);

        underTest = new RemoveOldestStrategy(storage);
    }

    @Test
    void removeOldest() {
        underTest.evict("key3");

        assertThat(dataForStorage.containsKey("key1"), is(false));
    }
}
