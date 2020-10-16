/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachingControllerTest {
    private static final String SERVICE_ID = "test-service";

    private CachingController underTest;

    private Storage mockStorage;

    @BeforeEach
    void setUp() {
        mockStorage = mock(Storage.class);
        underTest = new CachingController(mockStorage);
    }

    @Test
    void givenStorageReturnsValidValue_whenGetByKey_thenItReturnProperValue() {
        when(mockStorage.read(SERVICE_ID, "key")).thenReturn(new KeyValue("key", "value"));

        ResponseEntity<?> response = underTest.getValue("key");
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        KeyValue body = (KeyValue) response.getBody();
        assertThat(body.getValue(), is("value"));
    }

    @Test
    void givenStorageReturnsValidValues_whenGetByService_thenItReturnsProperValues() {
        Map<String, KeyValue> values = new HashMap<>();
        values.put("key", new KeyValue("key2", "value"));
        when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

        ResponseEntity<?> response = underTest.getAllValues();
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
        assertThat(result, is(values));
    }

    @Test
    void givenStorage_whenCreateKey_thenResponseOk() {
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.create(SERVICE_ID, keyValue)).thenReturn(keyValue);

        ResponseEntity<?> response = underTest.createKey(keyValue);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithKey_whenUpdateKey_thenResponseNoContent() {
        //TODO decide payload structure
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.update(SERVICE_ID, keyValue)).thenReturn(keyValue);

        ResponseEntity<?> response = underTest.update(keyValue);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithKey_whenDeleteKey_thenResponseNoContent() {
        when(mockStorage.delete(SERVICE_ID, "key")).thenReturn(new KeyValue("key", "value"));

        ResponseEntity<?> response = underTest.delete("key");
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }
}
