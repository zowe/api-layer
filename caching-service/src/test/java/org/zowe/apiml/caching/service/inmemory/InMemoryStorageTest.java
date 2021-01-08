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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InMemoryStorageTest {
    private InMemoryStorage underTest;
    private InMemoryConfig config;

    private Map<String, Map<String, KeyValue>> testingStorage;
    private final String serviceId = "acme";

    @BeforeEach
    void setUp() {
        testingStorage = new HashMap<>();
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy("reject");
        config = new InMemoryConfig(generalConfig);
        config.setMaxDataSize(10);
        underTest = new InMemoryStorage(config, testingStorage);
    }

    @Test
    void givenDefaultStorageConstructor_whenStorageConstructed_thenCanUseStorage() {
        underTest = new InMemoryStorage(config);
        underTest.create(serviceId, new KeyValue("key", "value"));

        KeyValue result = underTest.read(serviceId, "key");
        assertThat(result.getKey(), is("key"));
        assertThat(result.getValue(), is("value"));
    }

    @Test
    void givenThereIsNoValueForService_whenValueIsStored_thenItIsStored() {
        underTest.create(serviceId, new KeyValue("username", "ValidName"));

        KeyValue result = testingStorage.get(serviceId).get("username");
        assertThat(result.getKey(), is("username"));
        assertThat(result.getValue(), is("ValidName"));
    }

    @Test
    void givenThereIsValueForService_whenValueIsUpdated_thenItIsReplaced() {
        Map<String, KeyValue> serviceStorage = new HashMap<>();
        testingStorage.put(serviceId, serviceStorage);
        serviceStorage.put("username", new KeyValue("username", "Name 1"));
        underTest.update(serviceId, new KeyValue("username", "ValidName"));

        KeyValue result = testingStorage.get(serviceId).get("username");
        assertThat(result.getKey(), is("username"));
        assertThat(result.getValue(), is("ValidName"));
    }

    @Test
    void givenThereIsNoServiceCache_whenValueIsUpdated_thenNullIsReturned() {
        KeyValue keyValue = new KeyValue("username", "Name 1");
        assertThrows(StorageException.class, () -> {
            underTest.update(serviceId, keyValue);
        });
    }

    @Test
    void givenThereIsNoKey_whenValueIsUpdated_thenNullIsReturned() {
        testingStorage.put(serviceId, new HashMap<>());
        KeyValue keyValue = new KeyValue("bad key", "Name 1");
        assertThrows(StorageException.class, () -> {
            underTest.update(serviceId, keyValue);
        });
    }

    @Test
    void givenValueWasAlreadyAddedToTheStorage_whenRequested_thenItWillBeReturned() {
        Map<String, KeyValue> serviceStorage = new HashMap<>();
        testingStorage.put(serviceId, serviceStorage);
        serviceStorage.put("username", new KeyValue("username", "Name 1"));

        KeyValue result = underTest.read(serviceId, "username");
        assertThat(result.getKey(), is("username"));
        assertThat(result.getValue(), is("Name 1"));
    }

    @Test
    void givenNoValueWasStoredForTheService_whenRequested_thenNullWillBeReturned() {
        assertThrows(StorageException.class, () -> {
            underTest.read(serviceId, "username");
        });
    }

    @Test
    void givenServiceHasStoredValues_whenLoadingAllForService_thenAllAreReturned() {
        Map<String, KeyValue> serviceStorage = new HashMap<>();
        testingStorage.put(serviceId, serviceStorage);
        serviceStorage.put("username", new KeyValue("username", "Name 1"));

        Map<String, KeyValue> result = underTest.readForService(serviceId);
        assertThat(result.containsKey("username"), is(true));
    }

    @Test
    void givenKeyDoesntExist_whenDeletionRequested_thenNullIsReturned() {
        testingStorage.put(serviceId, new HashMap<>());
        assertThrows(StorageException.class, () -> {
            underTest.delete(serviceId, "nonexistent");
        });
    }

    @Test
    void givenServiceStorageDoesntExist_whenDeletionRequest_thenNullIsReturned() {
        assertThrows(StorageException.class, () -> {
            underTest.delete(serviceId, "nonexistent");
        });
    }

    @Test
    void givenKeyExists_whenDeletionRequested_thenKeyValueIsReturnedAndKeyIsRemoved() {
        Map<String, KeyValue> serviceStorage = new HashMap<>();
        testingStorage.put(serviceId, serviceStorage);
        serviceStorage.put("username", new KeyValue("username", "Name 1"));

        underTest.delete(serviceId, "username");
        assertThat(serviceStorage.containsKey("username"), is(false));
    }

    @Test
    void givenTheStorageIsFull_whenNewKeyValueIsAdded_thenTheInsufficientStorageExceptionIsRaised() {
        GeneralConfig generalConfig = new GeneralConfig();
        generalConfig.setEvictionStrategy("reject");
        config = new InMemoryConfig(generalConfig);
        config.setMaxDataSize(1);

        underTest = new InMemoryStorage(config);
                        underTest.create("customService", new KeyValue("key", "willFit"));
        KeyValue wontFit = new KeyValue("key", "wontFit");
        assertThrows(StorageException.class, () -> {
            underTest.create(serviceId, wontFit);
        });
    }
}
