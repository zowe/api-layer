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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.cache.PATRevocationStore;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.cache.StorageException;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.filter.CategorizeCertsFilter;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CachingControllerTest {
    private static final String BASE_PATH = "/cachingservice/api/v1";
    private static final String SERVICE_ID = "test-service";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String MAP_KEY = "map-key";

    private static final KeyValue KEY_VALUE = new KeyValue(KEY, VALUE);

    private Storage mockStorage;
    private final MessageService messageService = new YamlMessageService("/caching-log-messages.yml");
    private CachingController underTest;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        mockStorage = mock(Storage.class);
        underTest = new CachingController(mockStorage, messageService);
        underTest.maxQueryKeys = PATRevocationStore.DEFAULT_MAX_QUERY_KEYS;
        client = clientWithCertificate(SERVICE_ID);
    }

    private WebTestClient clientWithCertificate(String subject) {
        X509Certificate[] certificates = certificatesFor(subject);
        return WebTestClient.bindToController(underTest)
            .webFilter((exchange, chain) -> {
                exchange.getAttributes().put(CategorizeCertsFilter.ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE, certificates);
                return chain.filter(exchange);
            })
            .build();
    }

    private WebTestClient clientWithoutCertificate() {
        return WebTestClient.bindToController(underTest).build();
    }

    private static X509Certificate[] certificatesFor(String subject) {
        var principal = mock(X500Principal.class);
        when(principal.getName()).thenReturn(subject);
        var certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal()).thenReturn(principal);
        return new X509Certificate[]{certificate};
    }

    private ApiMessageView missingCertificateMessage() {
        return messageService.createMessage("org.zowe.apiml.cache.missingCertificate", "parameter").mapToView();
    }

    private static StorageException incompatibleStorage() {
        return new StorageException(Messages.INCOMPATIBLE_STORAGE_METHOD.getKey(), Messages.INCOMPATIBLE_STORAGE_METHOD.getStatus());
    }

    @Nested
    class WhenLoadingAllKeysForService {
        @Test
        void givenStorageReturnsValidValues_thenReturnProperValues() {
            Map<String, KeyValue> values = new HashMap<>();
            values.put(KEY, new KeyValue("key2", VALUE));
            when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

            client.get().uri(BASE_PATH + "/cache").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, KeyValue>>() {}).isEqualTo(values);
        }

        @Test
        void givenStorageThrowsInternalException_thenProperlyReturnError() {
            when(mockStorage.readForService(SERVICE_ID)).thenThrow(new RuntimeException());

            client.get().uri(BASE_PATH + "/cache").exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        void givenNoCertificate_thenReturnUnauthorized() {
            clientWithoutCertificate().get().uri(BASE_PATH + "/cache").exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiMessageView.class).isEqualTo(missingCertificateMessage());
            verifyNoInteractions(mockStorage);
        }
    }

    @Nested
    class WhenDeletingAllKeysForService {
        @Test
        void givenStorageRaisesNoException_thenReturnOk() {
            client.delete().uri(BASE_PATH + "/cache").exchange()
                .expectStatus().isOk();
            verify(mockStorage).deleteForService(SERVICE_ID);
        }

        @Test
        void givenStorageThrowsInternalException_thenProperlyReturnError() {
            doThrow(new RuntimeException()).when(mockStorage).deleteForService(SERVICE_ID);

            client.delete().uri(BASE_PATH + "/cache").exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    class WhenGetKey {
        @Test
        void givenStorageReturnsValidValue_thenReturnProperValue() {
            when(mockStorage.read(SERVICE_ID, KEY)).thenReturn(KEY_VALUE);

            client.get().uri(BASE_PATH + "/cache/" + KEY).exchange()
                .expectStatus().isOk()
                .expectBody(KeyValue.class).value(body -> assertThat(body.getValue(), is(VALUE)));
        }

        @Test
        void givenStoreWithNoKey_thenResponseNotFound() {
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
            when(mockStorage.read(any(), any())).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), new Exception("the cause"), KEY, SERVICE_ID));

            client.get().uri(BASE_PATH + "/cache/" + KEY).exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiMessageView.class).isEqualTo(expectedBody);
        }

        @Test
        void givenErrorReadingStorage_thenResponseInternalError() {
            when(mockStorage.read(any(), any())).thenThrow(new RuntimeException("error"));

            client.get().uri(BASE_PATH + "/cache/" + KEY).exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    class WhenCreateKey {
        @Test
        void givenStorage_thenResponseCreated() {
            when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

            client.post().uri(BASE_PATH + "/cache").bodyValue(KEY_VALUE).exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();
        }

        @Test
        void givenStorageWithExistingKey_thenResponseConflict() {
            when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenThrow(new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus(), KEY));
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyCollision", KEY).mapToView();

            client.post().uri(BASE_PATH + "/cache").bodyValue(KEY_VALUE).exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiMessageView.class).isEqualTo(expectedBody);
        }

        @Test
        void givenStorageWithError_thenResponseInternalError() {
            when(mockStorage.create(SERVICE_ID, KEY_VALUE)).thenThrow(new RuntimeException("error"));

            client.post().uri(BASE_PATH + "/cache").bodyValue(KEY_VALUE).exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        void givenNoPayload_thenResponseBadRequest() {
            client.post().uri(BASE_PATH + "/cache").contentType(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isBadRequest();
            verifyNoInteractions(mockStorage);
        }

        @Test
        void givenAnUnreadablePayload_thenResponseBadRequestRatherThanInternalError() {
            client.post().uri(BASE_PATH + "/cache").contentType(MediaType.APPLICATION_JSON).bodyValue("not json").exchange()
                .expectStatus().isBadRequest();
            verifyNoInteractions(mockStorage);
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.caching.api.CachingControllerTest#provideStringsForGivenVariousKeyValue")
        void givenVariousKeyValue_thenResponseAccordingly(String json, String errMessage, HttpStatus statusCode) {
            var response = client.post().uri(BASE_PATH + "/cache").contentType(MediaType.APPLICATION_JSON).bodyValue(json).exchange()
                .expectStatus().isEqualTo(statusCode);
            if (errMessage != null) {
                response.expectBody()
                    .jsonPath("$.messages[0].messageKey").isEqualTo("org.zowe.apiml.cache.invalidPayload")
                    .jsonPath("$.messages[0].messageContent").value(content -> assertThat((String) content, containsString(errMessage)));
            }
        }
    }

    private static Stream<Arguments> provideStringsForGivenVariousKeyValue() {
        return Stream.of(
            arguments("{\"key\":\"key\",\"value\":null}", "No value provided in the payload", HttpStatus.BAD_REQUEST),
            arguments("{\"key\":null,\"value\":\"value\"}", "No key provided in the payload", HttpStatus.BAD_REQUEST),
            arguments("{\"key\":\"key .%^&!@#\",\"value\":\"value\"}", null, HttpStatus.CREATED)
        );
    }

    @Nested
    class WhenUpdateKey {
        @Test
        void givenStorageWithKey_thenResponseNoContent() {
            when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenReturn(KEY_VALUE);

            client.put().uri(BASE_PATH + "/cache").bodyValue(KEY_VALUE).exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
        }

        @Test
        void givenStorageWithNoKey_thenResponseNotFound() {
            when(mockStorage.update(SERVICE_ID, KEY_VALUE)).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), KEY, SERVICE_ID));
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();

            client.put().uri(BASE_PATH + "/cache").bodyValue(KEY_VALUE).exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiMessageView.class).isEqualTo(expectedBody);
        }
    }

    @Nested
    class WhenDeleteKey {
        @Test
        void givenStorageWithKey_thenResponseNoContent() {
            when(mockStorage.delete(any(), any())).thenReturn(KEY_VALUE);

            client.delete().uri(BASE_PATH + "/cache/" + KEY).exchange()
                .expectStatus().isNoContent();
            verify(mockStorage).delete(SERVICE_ID, KEY);
        }

        @Test
        void givenStorageWithNoKey_thenResponseNotFound() {
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.keyNotInCache", KEY, SERVICE_ID).mapToView();
            when(mockStorage.delete(any(), any())).thenThrow(new StorageException(Messages.KEY_NOT_IN_CACHE.getKey(), Messages.KEY_NOT_IN_CACHE.getStatus(), KEY, SERVICE_ID));

            client.delete().uri(BASE_PATH + "/cache/" + KEY).exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiMessageView.class).isEqualTo(expectedBody);
        }
    }

    @Nested
    class WhenUseSpecificServiceHeader {
        @Test
        void givenNoServiceIdHeader_thenTheCertificateIdentifiesTheService() {
            Map<String, KeyValue> values = new HashMap<>();
            values.put(KEY, new KeyValue("key2", VALUE));
            when(mockStorage.readForService(SERVICE_ID)).thenReturn(values);

            client.get().uri(BASE_PATH + "/cache").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, KeyValue>>() {}).isEqualTo(values);
        }

        @Test
        void givenServiceIdHeaderAndCertificate_thenBothIdentifyTheService() {
            Map<String, KeyValue> values = new HashMap<>();
            values.put(KEY, new KeyValue("key2", VALUE));
            when(mockStorage.readForService("certificate, SERVICE=" + SERVICE_ID)).thenReturn(values);

            clientWithCertificate("certificate").get().uri(BASE_PATH + "/cache").header("X-CS-Service-ID", SERVICE_ID).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, KeyValue>>() {}).isEqualTo(values);
        }
    }

    @Nested
    class WhenInvalidatedTokenIsStored {
        @Test
        void givenCorrectPayload_thenStore() {
            client.post().uri(BASE_PATH + "/cache-list/" + MAP_KEY).bodyValue(KEY_VALUE).exchange()
                .expectStatus().isCreated()
                .expectBody().isEmpty();
            verify(mockStorage).storeMapItem(SERVICE_ID, MAP_KEY, KEY_VALUE);
        }

        @Test
        void givenIncorrectPayload_thenReturnBadRequest() {
            client.post().uri(BASE_PATH + "/cache-list/" + MAP_KEY).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"key\":null,\"value\":\"value\"}").exchange()
                .expectStatus().isBadRequest();
            verifyNoInteractions(mockStorage);
        }

        @Test
        void givenErrorOnTransaction_thenReturnInternalError() {
            when(mockStorage.storeMapItem(any(), any(), any()))
                .thenThrow(new StorageException(Messages.INTERNAL_SERVER_ERROR.getKey(), Messages.INTERNAL_SERVER_ERROR.getStatus(), new Exception("the cause"), KEY));

            client.post().uri(BASE_PATH + "/cache-list/" + MAP_KEY).bodyValue(KEY_VALUE).exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        void givenStorageWithExistingValue_thenResponseConflict() {
            when(mockStorage.storeMapItem(SERVICE_ID, MAP_KEY, KEY_VALUE))
                .thenThrow(new StorageException(Messages.DUPLICATE_VALUE.getKey(), Messages.DUPLICATE_VALUE.getStatus(), VALUE));
            ApiMessageView expectedBody = messageService.createMessage("org.zowe.apiml.cache.duplicateValue", VALUE).mapToView();

            client.post().uri(BASE_PATH + "/cache-list/" + MAP_KEY).bodyValue(KEY_VALUE).exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiMessageView.class).isEqualTo(expectedBody);
        }
    }

    @Nested
    class WhenRetrieveInvalidatedTokens {
        @Test
        void givenCorrectRequest_thenReturnList() {
            Map<String, String> expectedMap = Map.of("key", "token1", "key2", "token2");
            when(mockStorage.getAllMapItems(SERVICE_ID, MAP_KEY)).thenReturn(expectedMap);

            client.get().uri(BASE_PATH + "/cache-list/" + MAP_KEY).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, String>>() {}).isEqualTo(expectedMap);
        }

        @Test
        void givenCorrectRequest_thenReturnAllLists() {
            Map<String, Map<String, String>> expectedMap = Map.of(
                "invalidTokens", Map.of("key", "token1", "key2", "token2"),
                "invalidTokenRules", Map.of("key", "rule1", "key2", "rule2")
            );
            when(mockStorage.getAllMaps(SERVICE_ID)).thenReturn(expectedMap);

            client.get().uri(BASE_PATH + "/cache-list").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Map<String, String>>>() {}).isEqualTo(expectedMap);
        }

        @Test
        void givenNoCertificateInformation_thenReturnUnauthorized() {
            clientWithoutCertificate().get().uri(BASE_PATH + "/cache-list/" + MAP_KEY).exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiMessageView.class).isEqualTo(missingCertificateMessage());
        }
    }

    @Nested
    class WhenEvictRecord {
        @Test
        void givenCorrectRequest_thenRemoveTokensAndRules() {
            client.delete().uri(BASE_PATH + "/cache-list/evict/tokens/" + MAP_KEY).exchange()
                .expectStatus().isNoContent();
            verify(mockStorage).removeNonRelevantTokens(SERVICE_ID, MAP_KEY);

            client.delete().uri(BASE_PATH + "/cache-list/evict/rules/" + MAP_KEY).exchange()
                .expectStatus().isNoContent();
            verify(mockStorage).removeNonRelevantRules(SERVICE_ID, MAP_KEY);
        }

        @Test
        void givenInCorrectRequest_thenReturn500() {
            doThrow(new RuntimeException()).when(mockStorage).removeNonRelevantTokens(SERVICE_ID, MAP_KEY);
            doThrow(new RuntimeException()).when(mockStorage).removeNonRelevantRules(SERVICE_ID, MAP_KEY);

            client.delete().uri(BASE_PATH + "/cache-list/evict/tokens/" + MAP_KEY).exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            client.delete().uri(BASE_PATH + "/cache-list/evict/rules/" + MAP_KEY).exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        void givenIncompatibleStorage_thenReturnBadRequest() {
            doThrow(incompatibleStorage()).when(mockStorage).removeNonRelevantTokens(SERVICE_ID, MAP_KEY);
            doThrow(incompatibleStorage()).when(mockStorage).removeNonRelevantRules(SERVICE_ID, MAP_KEY);

            client.delete().uri(BASE_PATH + "/cache-list/evict/tokens/" + MAP_KEY).exchange()
                .expectStatus().isBadRequest();
            client.delete().uri(BASE_PATH + "/cache-list/evict/rules/" + MAP_KEY).exchange()
                .expectStatus().isBadRequest();
        }
    }

    @Nested
    class WhenGetAll {

        @Nested
        class MapItems {

            @Test
            void givenWrongStorage_whenGetAllMapItems_thenReturn400() {
                doThrow(incompatibleStorage()).when(mockStorage).getAllMapItems(any(), any());

                client.get().uri(BASE_PATH + "/cache-list/" + MAP_KEY).exchange()
                    .expectStatus().isBadRequest();
            }

            @Test
            void givenUnexpectedError_whenGetAllMapItems_thenReturn500() {
                doThrow(new RuntimeException("unexpected")).when(mockStorage).getAllMapItems(any(), any());

                client.get().uri(BASE_PATH + "/cache-list/" + MAP_KEY).exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            @Test
            void givenOtherStorageException_whenGetAllMapItems_thenReturnItsStatus() {
                doThrow(new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus()))
                    .when(mockStorage).getAllMapItems(any(), any());

                client.get().uri(BASE_PATH + "/cache-list/" + MAP_KEY).exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT);
            }

        }

        @Nested
        class Maps {

            @Test
            void givenWrongStorage_whenGetAllMaps_thenReturn400() {
                doThrow(incompatibleStorage()).when(mockStorage).getAllMaps(any());

                client.get().uri(BASE_PATH + "/cache-list").exchange()
                    .expectStatus().isBadRequest();
            }

            @Test
            void givenUnexpectedError_whenGetAllMaps_thenReturn500() {
                doThrow(new RuntimeException("unexpected")).when(mockStorage).getAllMaps(any());

                client.get().uri(BASE_PATH + "/cache-list").exchange()
                    .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            @Test
            void givenOtherStorageException_whenGetAllMaps_thenReturnItsStatus() {
                doThrow(new StorageException(Messages.DUPLICATE_KEY.getKey(), Messages.DUPLICATE_KEY.getStatus()))
                    .when(mockStorage).getAllMaps(any());

                client.get().uri(BASE_PATH + "/cache-list").exchange()
                    .expectStatus().isEqualTo(HttpStatus.CONFLICT);
            }

        }

    }

    @Nested
    class WhenTheStorageIsNotAvailable {

        @ParameterizedTest(name = "{0} {1}")
        @MethodSource("org.zowe.apiml.caching.api.CachingControllerTest#endpointsAndTheirStorageCalls")
        void thenEveryEndpointAnswersServiceUnavailable(HttpMethod method, String path, Consumer<Storage> storageCall) {
            storageCall.accept(doThrow(new StorageException(Messages.CACHE_NOT_AVAILABLE.getKey(), Messages.CACHE_NOT_AVAILABLE.getStatus(), "not ready"))
                .when(mockStorage));

            client.method(method).uri(BASE_PATH + path).exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static Stream<Arguments> endpointsAndTheirStorageCalls() {
        return Stream.of(
            arguments(HttpMethod.GET, "/cache", (Consumer<Storage>) storage -> storage.readForService(any())),
            arguments(HttpMethod.DELETE, "/cache", (Consumer<Storage>) storage -> storage.deleteForService(any())),
            arguments(HttpMethod.GET, "/cache/" + KEY, (Consumer<Storage>) storage -> storage.read(any(), any())),
            arguments(HttpMethod.DELETE, "/cache/" + KEY, (Consumer<Storage>) storage -> storage.delete(any(), any())),
            arguments(HttpMethod.GET, "/cache-list/" + MAP_KEY, (Consumer<Storage>) storage -> storage.getAllMapItems(any(), any())),
            arguments(HttpMethod.GET, "/cache-list", (Consumer<Storage>) storage -> storage.getAllMaps(any())),
            arguments(HttpMethod.GET, "/cache-list-legacy", (Consumer<Storage>) storage -> storage.getAllLegacyMaps(any())),
            arguments(HttpMethod.DELETE, "/cache-list/evict/rules/" + MAP_KEY, (Consumer<Storage>) storage -> storage.removeNonRelevantRules(any(), any())),
            arguments(HttpMethod.DELETE, "/cache-list/evict/tokens/" + MAP_KEY, (Consumer<Storage>) storage -> storage.removeNonRelevantTokens(any(), any()))
        );
    }

    @Nested
    class WhenQueryingSpecificItems {

        @Test
        void givenCorrectRequest_thenOnlyTheFoundEntriesAreReturned() {
            Map<String, Map<String, String>> found = Map.of("invalidTokens", Map.of("hash", "record"));
            when(mockStorage.getMapItems(any(), any())).thenReturn(found);

            client.post().uri(BASE_PATH + "/cache-query").bodyValue(Map.of("invalidTokens", List.of("hash"))).exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Map<String, String>>>() {}).isEqualTo(found);
        }

        @Test
        void givenMoreKeysThanTheLimit_thenReturnBadRequestNamingTheLimit() {
            List<String> tooMany = IntStream.rangeClosed(0, PATRevocationStore.DEFAULT_MAX_QUERY_KEYS)
                .mapToObj(i -> "hash" + i).toList();

            client.post().uri(BASE_PATH + "/cache-query").bodyValue(Map.of("invalidTokens", tooMany)).exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiMessageView.class)
                .value(body -> assertThat(body.getMessages().get(0).getMessageContent(), containsString(String.valueOf(underTest.maxQueryKeys))));
            verify(mockStorage, never()).getMapItems(any(), any());
        }

        @Test
        void givenNoPayload_thenReturnBadRequest() {
            client.post().uri(BASE_PATH + "/cache-query").contentType(MediaType.APPLICATION_JSON).exchange()
                .expectStatus().isBadRequest();
            verifyNoInteractions(mockStorage);
        }

        @Test
        void givenIncompatibleStorage_thenReturnBadRequest() {
            when(mockStorage.getMapItems(any(), any())).thenThrow(incompatibleStorage());

            client.post().uri(BASE_PATH + "/cache-query").bodyValue(Map.of()).exchange()
                .expectStatus().isBadRequest();
        }

        @Test
        void givenNoCertificateInformation_thenReturnUnauthorized() {
            clientWithoutCertificate().post().uri(BASE_PATH + "/cache-query").bodyValue(Map.of()).exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiMessageView.class).isEqualTo(missingCertificateMessage());
        }
    }

    @Nested
    class WhenReadingTheLegacyLayout {

        @Test
        void thenTheDedicatedStorageMethodIsUsedRatherThanTheCurrentOne() {
            Map<String, Map<String, String>> legacy = Map.of("invalidTokens", Map.of("hash", "record"));
            when(mockStorage.getAllLegacyMaps(SERVICE_ID)).thenReturn(legacy);

            client.get().uri(BASE_PATH + "/cache-list-legacy").exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Map<String, String>>>() {}).isEqualTo(legacy);
            verify(mockStorage, never()).getAllMaps(any());
        }

        @Test
        void givenIncompatibleStorage_thenReturnBadRequest() {
            when(mockStorage.getAllLegacyMaps(any())).thenThrow(incompatibleStorage());

            client.get().uri(BASE_PATH + "/cache-list-legacy").exchange()
                .expectStatus().isBadRequest();
        }

        @Test
        void givenNoCertificateInformation_thenReturnUnauthorized() {
            clientWithoutCertificate().get().uri(BASE_PATH + "/cache-list-legacy").exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ApiMessageView.class).isEqualTo(missingCertificateMessage());
        }
    }

}
