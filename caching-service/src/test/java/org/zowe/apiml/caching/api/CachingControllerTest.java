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
    void givenNoToken_whenGetByKey_thenResponseBadRequest() {
        //TODO
    }

    @Test
    void givenNoKey_whenGetByKey_thenResponseBadRequest() {
        ResponseEntity<?> response = underTest.getValue(null);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(nullValue())); //TODO have content?
    }

    @Test
    void givenInvalidToken_whenGetByKey_thenResponseNotAuthorized() {
        //TODO
    }

    @Test
    void givenStoreWithNoKey_whenGetByKey_thenResponseNotFound() {
        when(mockStorage.read(SERVICE_ID, "key")).thenReturn(null);

        ResponseEntity<?> response = underTest.getValue("key");
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(nullValue()));
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
    void givenNoToken_whenGetByService_thenResponseBadRequest() {
        //TODO
    }

    @Test
    void givenInvalidToken_whenGetByService_thenResponseNotAuthorized() {
        //TODO
    }

    @Test
    void givenStorage_whenCreateKey_thenResponseCreated() {
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.create(SERVICE_ID, keyValue)).thenReturn(keyValue);

        ResponseEntity<?> response = underTest.createKey(keyValue);
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenInvalidJson_whenCreateKey_thenResponseBadRequest() {
        ResponseEntity<?> response = underTest.createKey(null);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenNoToken_whenCreateKey_thenResponseBadRequest() {
        //TODO
    }

    @Test
    void givenInvalidToken_whenCreateKey_thenResponseNotAuthorized() {
        //TODO
    }

    @Test
    void givenStorageWithExistingKey_whenCreateKey_thenResponseConflict() {
        KeyValue keyValue = new KeyValue("key", "value");
        //TODO mock storage method to show conflict
        underTest.createKey(keyValue);

        ResponseEntity<?> response = underTest.createKey(keyValue);
        assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
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
    void givenInvalidJson_whenUpdateKey_thenResponseBadRequest() {
        ResponseEntity<?> response = underTest.update(null);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenNoToken_whenUpdateKey_thenResponseBadRequest() {
        //TODO
    }

    @Test
    void givenInvalidToken_whenUpdateKey_thenResponseNotAuthorized() {
        //TODO
    }

    @Test
    void givenStorageWithNoKey_whenUpdateKey_thenResponseNotFound() {
        KeyValue keyValue = new KeyValue("key", "value");
        //TODO mock storage method to show doesn't exist

        ResponseEntity<?> response = underTest.update(keyValue);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithKey_whenDeleteKey_thenResponseNoContent() {
        when(mockStorage.delete(SERVICE_ID, "key")).thenReturn(new KeyValue("key", "value"));

        ResponseEntity<?> response = underTest.delete("key");
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenNoKey_whenDeleteKey_thenResponseBadRequest() {
        ResponseEntity<?> response = underTest.delete(null);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenNoToken_whenDeleteKey_thenResponseBadRequest() {
        //TODO
    }

    @Test
    void givenInvalidToken_whenDeleteKey_thenResponseNotAuthorized() {
        //TODO
    }

    @Test
    void givenStorageWithNoKey_whenDeleteKey_thenResponseNotFound() {
        //TODO mock storage method to show doesn't exist

        ResponseEntity<?> response = underTest.delete("key");
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(nullValue()));
    }
}
