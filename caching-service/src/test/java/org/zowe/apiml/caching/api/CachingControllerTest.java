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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CachingControllerTest {
    private static final String SERVICE_ID = "test-service";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String MAP_KEY = "map-key";

    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);

    private ServerHttpRequest mockRequest;
    private Storage mockStorage;
    private final MessageService messageService = new YamlMessageService("/caching-log-messages.yml");
    private CachingController underTest;

    @BeforeEach
    void setUp() {
        mockRequest = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Certificate-DistinguishedName", SERVICE_ID);
        headers.add("X-CS-Service-ID", null);
        when(mockRequest.getHeaders()).thenReturn(headers);
        when(mockRequest.getURI()).thenReturn(URI.create("http://localhost"));
        mockStorage = mock(Storage.class);
        underTest = new CachingController(mockStorage, messageService);
    }

    @Nested
    class WhenLoadingAllKeysForService {
        @Test
        void givenStorageReturnsValidValues_thenReturnProperValues() {
            Map<String, KeyValue> values = new HashMap<>();
            values.put(KEY, new KeyValue("key2", VALUE));
            when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

            StepVerifier.create(underTest.getAllValues(mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                    Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
                    assertThat(result, is(values));
                })
                .verifyComplete();
        }

        @Test
        void givenStorageThrowsInternalException_thenProperlyReturnError() {
            when(mockStorage.readForService(SERVICE_ID)).thenThrow(new RuntimeException());

            StepVerifier.create(underTest.getAllValues(mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();
        }
    }

    @Nested
    class WhenDeletingAllKeysForService {
        @Test
        void givenStorageRaisesNoException_thenReturnOk() {
            StepVerifier.create(underTest.deleteAllValues(mockRequest))
                .assertNext(response -> {
                    verify(mockStorage).deleteForService(SERVICE_ID);
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                })
                .verifyComplete();
        }

        @Test
        void givenStorageThrowsInternalException_thenProperlyReturnError() {
            when(mockStorage.readForService(SERVICE_ID)).thenThrow(new RuntimeException());

            StepVerifier.create(underTest.getAllValues(mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();
        }
    }

    @Nested
    class WhenGetKey {
        @Test
        void givenStorageReturnsValidValue_thenReturnProperValue() {
            when(mockStorage.read(SERVICE_ID, KEY)).thenReturn(KEY_VALUE);

            StepVerifier.create(underTest.getValue(KEY, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                    KeyValue body = (KeyValue) response.getBody();
                    assertThat(body, notNullValue());
                    assertThat(body.getValue(), is(VALUE));
                })
                .verifyComplete();
        }

        @Test
        void givenNoKey_thenResponseBadRequest() {
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided", SERVICE_ID).mapToView();

            StepVerifier.create(underTest.getValue(null, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }

        @Test
        void givenStoreWithNoKey_thenResponseNotFound() {
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
            when(mockStorage.read(any(), any())).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), new Exception("the cause"), KEY, SERVICE_ID));

            StepVerifier.create(underTest.getValue(KEY, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }

        @Test
        void givenErrorReadingStorage_thenResponseInternalError() {
            when(mockStorage.read(any(), any())).thenThrow(new RuntimeException("error"));

            StepVerifier.create(underTest.getValue(KEY, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();
        }
    }

    @Nested
    class WhenCreateKey {
        @Test
        void givenStorage_thenResponseCreated() {
            when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

            StepVerifier.create(underTest.createKey(KEY_VALUE, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
                    assertThat(response.getBody(), is(nullValue()));
                })
                .verifyComplete();
        }

        @Test
        void givenStorageWithExistingKey_thenResponseConflict() {
            when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenThrow(new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), KEY));
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyCollision", KEY).mapToView();

            StepVerifier.create(underTest.createKey(KEY_VALUE, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }

        @Test
        void givenStorageWithError_thenResponseInternalError() {
            when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenThrow(new RuntimeException("error"));

            StepVerifier.create(underTest.createKey(KEY_VALUE, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();
        }
    }

    @Nested
    class WhenUpdateKey {
        @Test
        void givenStorageWithKey_thenResponseNoContent() {
            when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

            StepVerifier.create(underTest.update(KEY_VALUE, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
                    assertThat(response.getBody(), is(nullValue()));
                })
                .verifyComplete();
        }

        @Test
        void givenStorageWithNoKey_thenResponseNotFound() {
            when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), KEY, SERVICE_ID));
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();

            StepVerifier.create(underTest.update(KEY_VALUE, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }
    }

    @Nested
    class WhenDeleteKey {
        @Test
        void givenStorageWithKey_thenResponseNoContent() {
            when(mockStorage.delete(any(), any())).thenReturn(KEY_VALUE);

            StepVerifier.create(underTest.delete(KEY, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
                    assertThat(response.getBody(), is(KEY_VALUE));
                })
                .verifyComplete();
        }

        @Test
        void givenNoKey_thenResponseBadRequest() {
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotProvided").mapToView();

            StepVerifier.create(underTest.delete(null, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }

        @Test
        void givenStorageWithNoKey_thenResponseNotFound() {
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
            when(mockStorage.delete(any(), any())).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), KEY, SERVICE_ID));

            StepVerifier.create(underTest.delete(KEY, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }
    }

    @Test
    void givenNoPayload_whenValidatePayload_thenResponseBadRequest() {
        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload", null, "No KeyValue provided in the payload").mapToView();

        StepVerifier.create(underTest.createKey(null, mockRequest))
            .assertNext(response -> {
                assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
                assertThat(response.getBody(), is(expectedBody));
            })
            .verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("provideStringsForGivenVariousKeyValue")
    void givenVariousKeyValue_whenValidatePayload_thenResponseAccordingly(String key, String value, String errMessage, HttpStatus statusCode) {
        KeyValue keyValue = new KeyValue(key, value);

        StepVerifier.create(underTest.createKey(keyValue, mockRequest))
            .assertNext(response -> {
                assertThat(response.getStatusCode(), is(statusCode));
                if (errMessage != null) {
                    ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.invalidPayload", keyValue, errMessage).mapToView();
                    assertThat(response.getBody(), is(expectedBody));
                }
            })
            .verifyComplete();
    }

    private static Stream<Arguments> provideStringsForGivenVariousKeyValue() {
        return Stream.of(
            Arguments.of("key", null, "No value provided in the payload", HttpStatus.BAD_REQUEST),
            Arguments.of(null, "value", "No key provided in the payload", HttpStatus.BAD_REQUEST),
            Arguments.of("key .%^&!@#", "value", null, HttpStatus.CREATED)
        );
    }

    @Test
    void givenNoCertificateInformationInHeader_whenGetAllValues_thenReturnUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Certificate-DistinguishedName", null);
        when(mockRequest.getHeaders()).thenReturn(headers);

        ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.missingCertificate",
            "parameter").mapToView();
        StepVerifier.create(underTest.getAllValues(mockRequest))
            .assertNext(response -> {
                assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
                assertThat(response.getBody(), is(expectedBody));
            })
            .verifyComplete();
    }

    @Nested
    class WhenUseSpecificServiceHeader {
        @BeforeEach
        void setUp() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Certificate-DistinguishedName", SERVICE_ID);
            headers.add("X-CS-Service-ID", null);
            when(mockRequest.getHeaders()).thenReturn(headers);
        }

        @Test
        void givenServiceIdHeader_thenReturnProperValues() {

            Map<String, KeyValue> values = new HashMap<>();
            values.put(KEY, new KeyValue("key2", VALUE));
            when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

            StepVerifier.create(underTest.getAllValues(mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                    Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
                    assertThat(result, is(values));
                })
                .verifyComplete();
        }

        @Test
        void givenServiceIdHeaderAndCertificateHeaderForReadForService_thenReturnProperValues() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Certificate-DistinguishedName", "certificate");
            headers.add("X-CS-Service-ID", SERVICE_ID);
            when(mockRequest.getHeaders()).thenReturn(headers);

            Map<String, KeyValue> values = new HashMap<>();
            values.put(KEY, new KeyValue("key2", VALUE));
            when(mockStorage.readForService("certificate, SERVICE=" + SERVICE_ID)).thenReturn(values);

            StepVerifier.create(underTest.getAllValues(mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                    Map<String, KeyValue> result = (Map<String, KeyValue>) response.getBody();
                    assertThat(result, is(values));
                })
                .verifyComplete();
        }
    }

    @Nested
    class WhenInvalidatedTokenIsStored {
        @Test
        void givenCorrectPayload_thenStore() {
            StepVerifier.create(underTest.storeMapItem(MAP_KEY, KEY_VALUE, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
                    assertThat(response.getBody(), is(nullValue()));
                })
                .verifyComplete();
        }

        @Test
        void givenIncorrectPayload_thenReturnBadRequest() {
            KeyValue keyValue = new KeyValue(null, VALUE);

            StepVerifier.create(underTest.storeMapItem(MAP_KEY, keyValue, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST)))
                .verifyComplete();
        }

        @Test
        void givenErrorOnTransaction_thenReturnInternalError() throws StorageException {
            when(mockStorage.storeMapItem(any(), any(), any()))
                .thenThrow(new StorageException(Messages.INTERNAL_SERVER_ERROR.getKey(), Messages.INTERNAL_SERVER_ERROR.getStatus(), new Exception("the cause"), KEY));

            StepVerifier.create(underTest.storeMapItem(MAP_KEY, KEY_VALUE, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();
        }

        @Test
        void givenStorageWithExistingValue_thenResponseConflict() throws StorageException {
            when(mockStorage.storeMapItem(SERVICE_ID, MAP_KEY, KEY_VALUE))
                .thenThrow(new StorageException(Messages.DUPLICATE_VALUE.getKey(), Messages.DUPLICATE_VALUE.getStatus(), VALUE));

            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.duplicateValue", VALUE).mapToView();

            StepVerifier.create(underTest.storeMapItem(MAP_KEY, KEY_VALUE, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.CONFLICT));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }
    }

    @Nested
    class WhenRetrieveInvalidatedTokens {
        @Test
        void givenCorrectRequest_thenReturnList() throws StorageException {
            HashMap<String, String> expectedMap = new HashMap<>();
            expectedMap.put("key", "token1");
            expectedMap.put("key2", "token2");

            when(mockStorage.getAllMapItems(anyString(), any())).thenReturn(expectedMap);

            StepVerifier.create(underTest.getAllMapItems(MAP_KEY, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                    assertThat(response.getBody(), is(expectedMap));
                })
                .verifyComplete();
        }

        @Test
        void givenCorrectRequest_thenReturnAllLists() throws StorageException {
            Map<String, Map<String, String>> expectedMap = getStringMapMap();

            when(mockStorage.getAllMaps(anyString())).thenReturn(expectedMap);

            StepVerifier.create(underTest.getAllMaps(mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.OK));
                    assertThat(response.getBody(), is(expectedMap));
                })
                .verifyComplete();
        }

        private static Map<String, Map<String, String>> getStringMapMap() {
            Map<String, String> invalidTokens = new HashMap<>();
            invalidTokens.put("key", "token1");
            invalidTokens.put("key2", "token2");

            Map<String, String> invalidTokenRules = new HashMap<>();
            invalidTokenRules.put("key", "rule1");
            invalidTokenRules.put("key2", "rule2");

            Map<String, Map<String, String>> expectedMap = new HashMap<>();
            expectedMap.put("invalidTokens", invalidTokens);
            expectedMap.put("invalidTokenRules", invalidTokenRules);
            return expectedMap;
        }

        @Test
        void givenNoCertificateInformation_thenReturnUnauthorized() throws StorageException {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Certificate-DistinguishedName", null);
            when(mockRequest.getHeaders()).thenReturn(headers);

            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.missingCertificate", "parameter").mapToView();

            StepVerifier.create(underTest.getAllMapItems(MAP_KEY, mockRequest))
                .assertNext(response -> {
                    assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
                    assertThat(response.getBody(), is(expectedBody));
                })
                .verifyComplete();
        }

        @Test
        void givenErrorReadingStorage_thenResponseBadRequest() throws StorageException {
            when(mockStorage.getAllMapItems(anyString(), anyString()))
                .thenThrow(new RuntimeException("error"));

            StepVerifier.create(underTest.getAllMapItems(MAP_KEY, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST)))
                .verifyComplete();
        }
    }

    @Nested
    class WhenEvictRecord {
        @Test
        void givenCorrectRequest_thenRemoveTokensAndRules() throws StorageException {
            StepVerifier.create(underTest.evictTokens(MAP_KEY, mockRequest))
                .assertNext(response -> {
                    verify(mockStorage).removeNonRelevantTokens(SERVICE_ID, MAP_KEY);
                    assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
                })
                .verifyComplete();

            StepVerifier.create(underTest.evictRules(MAP_KEY, mockRequest))
                .assertNext(response -> {
                    verify(mockStorage).removeNonRelevantRules(SERVICE_ID, MAP_KEY);
                    assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
                })
                .verifyComplete();
        }

        @Test
        void givenInCorrectRequest_thenReturn500() throws StorageException {
            doThrow(new RuntimeException()).when(mockStorage).removeNonRelevantTokens(SERVICE_ID, MAP_KEY);
            doThrow(new RuntimeException()).when(mockStorage).removeNonRelevantRules(SERVICE_ID, MAP_KEY);

            StepVerifier.create(underTest.evictTokens(MAP_KEY, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();

            StepVerifier.create(underTest.evictRules(MAP_KEY, mockRequest))
                .assertNext(response -> assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR)))
                .verifyComplete();
        }
    }
}
