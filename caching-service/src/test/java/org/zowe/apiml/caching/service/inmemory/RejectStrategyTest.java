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
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.inmemory.config.InMemoryConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RejectStrategyTest {
    private RejectStrategy underTest;
    private Map<String, KeyValue> serviceSpecificStorage;

    @BeforeEach
    void setUp() {
        Map<String, Map<String, KeyValue>> storage = new ConcurrentHashMap<>();
        serviceSpecificStorage = new ConcurrentHashMap<>();
        storage.put("test-service", serviceSpecificStorage);

        InMemoryConfig config = new InMemoryConfig(new GeneralConfig());
        config.setMaxDataSize(1);
        underTest = new RejectStrategy(storage, config);
    }


    @Test
    void givenThereIsLessThanConfiguredItems_whenAboveThresholdIsConsulted_thenFalseIsReturned() {
        assertThat(underTest.aboveThreshold(), is(false));
    }

    @Test
    void givenThereIsMoreThanConfiguredItems_whenAboveThresholdIsConsulted_thenFalseIsReturned() {
        serviceSpecificStorage.put("key1", new KeyValue("key1", "value1"));
        serviceSpecificStorage.put("key2", new KeyValue("key2", "value2"));

        assertThat(underTest.aboveThreshold(), is(true));
    }

    @Test
    void evictThrowsException() {
        assertThrows(StorageException.class, () -> {
            underTest.evict("anyValue");
        });
    }
}
