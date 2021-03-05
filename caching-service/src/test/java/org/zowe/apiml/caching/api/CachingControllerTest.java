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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachingControllerTest {
    private static final String SERVICE_ID = "test-service";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);

    private HttpServletRequest mockRequest;
    private HttpServletRequest httpServletRequest;
    private Storage mockStorage;
    private final MessageService messageService = new YamlMessageService("/caching-log-messages.yml");
    private CachingController underTest;

    static Stream<String> emptyStrings() {
        return Stream.of("", null);
    }

    @BeforeEach
    void setUp() {
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("X-Certificate-DistinguishedName")).thenReturn("test-service");
        mockStorage = mock(Storage.class);
        httpServletRequest = mock(HttpServletRequest.class);
        underTest = new CachingController(mockStorage, messageService);
    }

    @Test
    void givenStorageReturnsValidValue_whenGetByKey_thenReturnProperValue() {
        when(mockStorage.read(SERVICE_ID, KEY)).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        KeyValue body = (KeyValue) response.getBody();
        assertThat(body.getValue(), is(VALUE));
    }


    @Test
    void givenNoKey_whenGetByKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided", SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.getValue(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStoreWithNoKey_whenGetByKey_thenResponseNotFound() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
        when(mockStorage.read(any(), any())).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), new Exception("the cause"), KEY, SERVICE_ID));

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenErrorReadingStorage_whenGetByKey_thenResponseInternalError() {
        when(mockStorage.read(any(), any())).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = underTest.getValue(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void givenStorageReturnsValidValues_whenGetByService_thenReturnProperValues() {
        Map<String, KeyValue> values = new HashMap<>();
        values.put(KEY, new KeyValue("key2", VALUE));
        when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

        ResponseEntity<?> response = underTest.getAllValues(mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
        assertThat(result, is(values));
    }


    @Test
    void givenStorage_whenCreateKey_thenResponseCreated() {
        when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.createKey(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithExistingKey_whenCreateKey_thenResponseConflict() {
        when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenThrow(new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), KEY));
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyCollision", KEY).mapToView();

        ResponseEntity<?> response = underTest.createKey(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithError_whenCreateKey_thenResponseInternalError() {
        when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = underTest.createKey(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
    }


    @Test
    void givenStorageWithKey_whenUpdateKey_thenResponseNoContent() {
        when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.update(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenStorageWithNoKey_whenUpdateKey_thenResponseNotFound() {
        when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), KEY, SERVICE_ID));
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.update(KEY_VALUE, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithKey_whenDeleteKey_thenResponseNoContent() {
        when(mockStorage.delete(any(), any())).thenReturn(KEY_VALUE);

        ResponseEntity<?> response = underTest.delete(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(KEY_VALUE));
    }

    @Test
    void givenNoKey_whenDeleteKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided").mapToView();

        ResponseEntity<?> response = underTest.delete(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithNoKey_whenDeleteKey_thenResponseNotFound() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
        when(mockStorage.delete(any(), any())).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), KEY, SERVICE_ID));

        ResponseEntity<?> response = underTest.delete(KEY, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoPayload_whenValidatePayload_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload",
            null, "No KeyValue provided in the payload").mapToView();

        ResponseEntity<?> response = underTest.createKey(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @ParameterizedTest
    @MethodSource("provideStringsForGivenBadKeyValue")
    void givenBadKeyValue_whenValidatePayload_thenResponseBadRequest(String key, String value, String errMessage) {
        KeyValue keyValue = new KeyValue(key, value);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload",
            keyValue, errMessage).mapToView();

        ResponseEntity<?> response = underTest.createKey(keyValue, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    private static Stream<Arguments> provideStringsForGivenBadKeyValue() {
        return Stream.of(
            Arguments.of("key", null, "No value provided in the payload"),
            Arguments.of(null, "value", "No key provided in the payload"),
            Arguments.of("key ", "value", "Key is not alphanumeric")
        );
    }

    @ParameterizedTest
    @MethodSource("emptyStrings")
    void givenNoCertificateInformationInHeader_whenGetAllValues_thenReturnUnauthorized() {
        when(mockStorage.read(SERVICE_ID, KEY)).thenReturn(KEY_VALUE);
        when(httpServletRequest.getHeader("X-Certificate-DistinguishedName")).thenReturn(null);
        ResponseEntity<?> response = underTest.getAllValues(httpServletRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.missingCertificate",
            "parameter").mapToView();
        assertThat(response.getBody(), is(expectedBody));
    }

}
