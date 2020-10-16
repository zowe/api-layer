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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InMemoryStorageTest {
    private InMemoryStorage underTest;

    private Map<String, Map<String, KeyValue>> testingStorage;
    private String serviceId = "acme";

    @BeforeEach
    void setUp() {
        testingStorage = new HashMap<>();
        underTest = new InMemoryStorage(testingStorage);
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
        KeyValue result = underTest.read(serviceId, "username");
        assertThat(result, is(nullValue()));
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
        KeyValue result = underTest.delete(serviceId, "nonexistent");
        assertThat(result, is(nullValue()));
    }

    @Test
    void givenKeyExists_whenDeletionRequested_thenKeyValueIsReturnedAndKeyIsRemoved() {
        Map<String, KeyValue> serviceStorage = new HashMap<>();
        testingStorage.put(serviceId, serviceStorage);
        serviceStorage.put("username", new KeyValue("username", "Name 1"));

        underTest.delete(serviceId, "username");
        assertThat(serviceStorage.containsKey("username"), is(false));
    }
    // Remove existing key
}
