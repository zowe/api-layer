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
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.service.ZaasClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CachingControllerTest {
    private static final String SERVICE_ID = "test-service";

    private Storage mockStorage;
    private ZaasClient mockZaasClient;
    private final MessageService messageService = new YamlMessageService();
    private CachingController underTest;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();

        mockStorage = mock(Storage.class);
        mockZaasClient = mock(ZaasClient.class);
        underTest = new CachingController(mockStorage, mockZaasClient, messageService);
    }

    @Test
    void givenStorageReturnsValidValue_whenGetByKey_thenReturnProperValue() {
        when(mockStorage.read(SERVICE_ID, "key")).thenReturn(new KeyValue("key", "value"));

        ResponseEntity<?> response = underTest.getValue("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        KeyValue body = (KeyValue) response.getBody();
        assertThat(body.getValue(), is("value"));
    }

    @Test
    void givenNoToken_whenGetByKey_thenResponseBadRequest() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED));

        ResponseEntity<?> response = underTest.getValue("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoKey_whenGetByKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided", SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.getValue(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenInvalidToken_whenGetByKey_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN));

        ResponseEntity<?> response = underTest.getValue("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStoreWithNoKey_whenGetByKey_thenResponseNotFound() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", SERVICE_ID).mapToView();
        when(mockStorage.read(SERVICE_ID, "key")).thenReturn(null);

        ResponseEntity<?> response = underTest.getValue("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageReturnsValidValues_whenGetByService_thenReturnProperValues() {
        Map<String, KeyValue> values = new HashMap<>();
        values.put("key", new KeyValue("key2", "value"));
        when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

        ResponseEntity<?> response = underTest.getAllValues(mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));

        Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
        assertThat(result, is(values));
    }

    @Test
    void givenNoToken_whenGetByService_thenResponseBadRequest() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED));

        ResponseEntity<?> response = underTest.getAllValues(mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenInvalidToken_whenGetByService_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN));

        ResponseEntity<?> response = underTest.getAllValues(mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorage_whenCreateKey_thenResponseCreated() {
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.create(SERVICE_ID, keyValue)).thenReturn(keyValue);

        ResponseEntity<?> response = underTest.createKey(keyValue, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenInvalidPayload_whenCreateKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload", Collections.singleton(null)).mapToView();

        ResponseEntity<?> response = underTest.createKey(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoToken_whenCreateKey_thenResponseBadRequest() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED));

        ResponseEntity<?> response = underTest.createKey(new KeyValue("key", "value"), mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenInvalidToken_whenCreateKey_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN));

        ResponseEntity<?> response = underTest.createKey(new KeyValue("key", "value"), mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithExistingKey_whenCreateKey_thenResponseConflict() {
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.create(SERVICE_ID, keyValue)).thenReturn(null);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyCollision", SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.createKey(keyValue, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithKey_whenUpdateKey_thenResponseNoContent() {
        //TODO decide payload structure
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.update(SERVICE_ID, keyValue)).thenReturn(keyValue);

        ResponseEntity<?> response = underTest.update(keyValue, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenInvalidPayload_whenUpdateKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload", Collections.singleton(null)).mapToView();

        ResponseEntity<?> response = underTest.update(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoToken_whenUpdateKey_thenResponseBadRequest() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided").mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED));

        ResponseEntity<?> response = underTest.update(new KeyValue("key", "value"), mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenInvalidToken_whenUpdateKey_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN));

        ResponseEntity<?> response = underTest.getAllValues(mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithNoKey_whenUpdateKey_thenResponseNotFound() {
        KeyValue keyValue = new KeyValue("key", "value");
        when(mockStorage.update(SERVICE_ID, keyValue)).thenReturn(null);
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", "key", SERVICE_ID).mapToView();

        ResponseEntity<?> response = underTest.update(keyValue, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithKey_whenDeleteKey_thenResponseNoContent() {
        when(mockStorage.delete(SERVICE_ID, "key")).thenReturn(new KeyValue("key", "value"));

        ResponseEntity<?> response = underTest.delete("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
        assertThat(response.getBody(), is(nullValue()));
    }

    @Test
    void givenNoKey_whenDeleteKey_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided").mapToView();

        ResponseEntity<?> response = underTest.delete(null, mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenNoToken_whenDeleteKey_thenResponseBadRequest() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.tokenNotProvided", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED));

        ResponseEntity<?> response = underTest.delete("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenInvalidToken_whenDeleteKey_thenResponseUnauthorized() throws ZaasClientException {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.security.query.invalidToken", mockRequest.getRequestURL().toString()).mapToView();
        when(mockZaasClient.query(any())).thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_JWT_TOKEN));

        ResponseEntity<?> response = underTest.delete("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
        assertThat(response.getBody(), is(expectedBody));
    }

    @Test
    void givenStorageWithNoKey_whenDeleteKey_thenResponseNotFound() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", "key", SERVICE_ID).mapToView();
        when(mockStorage.delete(SERVICE_ID, "key")).thenReturn(null);

        ResponseEntity<?> response = underTest.delete("key", mockRequest);
        assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
        assertThat(response.getBody(), is(expectedBody));
    }
}
