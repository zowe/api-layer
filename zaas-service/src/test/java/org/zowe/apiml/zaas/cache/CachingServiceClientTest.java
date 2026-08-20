/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachingServiceClientTest {
    CachingServiceClient underTest;
    RestTemplate restTemplate = mock(RestTemplate.class);
    String urlBase = "https://localhost:10010/cachingservice/api/v1/cache";

    @BeforeEach
    void setUp() {
        ServiceAddress gatewayAddress = ServiceAddress.builder().scheme("https").hostname("localhost:10010").build();
        GatewayClient gatewayClient = new GatewayClient(gatewayAddress);
        underTest = new CachingServiceClient(restTemplate, gatewayClient);
        ReflectionTestUtils.setField(underTest, "CACHING_API_PATH", "/cachingservice/api/v1/cache");
        ReflectionTestUtils.setField(underTest, "CACHING_LIST_API_PATH", "/cachingservice/api/v1/cache-list/");
        ReflectionTestUtils.setField(underTest, "CACHING_LEGACY_LIST_API_PATH", "/cachingservice/api/v1/cache-list-legacy");
        ReflectionTestUtils.setField(underTest, "CACHING_QUERY_API_PATH", "/cachingservice/api/v1/cache-query");
    }

    @Nested
    class givenCreateOperation {

        @Test
        void createWithoutProblem() {
            CachingServiceClient.KeyValue kv = new CachingServiceClient.KeyValue("Britney", "Spears");
            assertDoesNotThrow(() -> underTest.create(kv));
            verify(restTemplate).exchange(urlBase, HttpMethod.POST, new HttpEntity<>(kv, CachingServiceClient.getDefaultHeaders()), String.class);
        }

        @Test
        void createWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.create(new CachingServiceClient.KeyValue("Britney", "Spears")));
        }
    }

    @Nested
    class givenUpdateOperation {
        @Test
        void updateWithoutProblem() {
            CachingServiceClient.KeyValue kv = new CachingServiceClient.KeyValue("Britney", "Speeeeers");
            assertDoesNotThrow(() -> underTest.update(kv));
            verify(restTemplate).exchange(urlBase, HttpMethod.PUT, new HttpEntity<>(kv, CachingServiceClient.getDefaultHeaders()), String.class);
        }

        @Test
        void updateWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.update(new CachingServiceClient.KeyValue("Britney", "Spears")));
        }
    }

    @Nested
    class givenReadOperation {

        private String keyToRead = "reee";

        @Test
        void readWithNullResponseOrNullBody() {
            ResponseEntity<CachingServiceClient.KeyValue> responseEntity = mock(ResponseEntity.class);
            doReturn(false).when(responseEntity).hasBody();
            doReturn(responseEntity).when(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
            verify(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
        }

        @Test
        void readWithoutProblem() throws CachingServiceClientException {
            ResponseEntity<CachingServiceClient.KeyValue> responseEntity = mock(ResponseEntity.class);
            doReturn(true).when(responseEntity).hasBody();
            doReturn(new CachingServiceClient.KeyValue(keyToRead, "Wonder")).when(responseEntity).getBody();
            doReturn(responseEntity).when(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            assertThat(underTest.read(keyToRead).getValue(), is("Wonder"));
        }

        @Test
        void readWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
        }

        @Test
        void ioException() {
            RestClientException ioException = new RestClientException("io");
            doThrow(ioException).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            CachingServiceClientException e = assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
            assertSame(e.getCause(), ioException);
        }

        @Test
        void notFound() {
            doThrow(HttpClientErrorException.create("record not found", HttpStatus.NOT_FOUND, "notFound", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8))
                .when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            CachingServiceClientException e = assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
            assertNull(e.getCause());
        }

        @Test
        void noAvailable() {
            Exception responseException = HttpClientErrorException.create("service not available", HttpStatus.SERVICE_UNAVAILABLE, "503", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
            doThrow(responseException).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            CachingServiceClientException e = assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
            assertSame(e.getCause(), responseException);
        }

    }

    @Nested
    class givenDeleteOperation {
        private String keyToDelete = "reee";

        @Test
        void deleteWithoutProblem() {
            assertDoesNotThrow(() -> underTest.delete(keyToDelete));
            verify(restTemplate).exchange(eq(urlBase + "/" + keyToDelete), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class));
        }

        @Test
        void deleteWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.delete(keyToDelete));
        }
    }

    @Nested
    class GivenAppendListTest {
        ResponseEntity<Map<String, Map<String, String>>> response;

        @BeforeEach
        void setup() {
            ParameterizedTypeReference<Map<String, Map<String, String>>> responseType =
                new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
                };
            response = (ResponseEntity<Map<String, Map<String, String>>>) mock(ResponseEntity.class);
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(responseType))).thenReturn(response);
        }

        @Test
        void whenClientReturnsBody_thenParseTheResponse() throws CachingServiceClientException, JsonProcessingException {
            String key = "token";
            AccessTokenContainer container = new AccessTokenContainer(null, key, null, null, null, null);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> tokens = new HashMap<>();
            String json = mapper.writeValueAsString(container);
            tokens.put(key, json);
            Map<String, Map<String, String>> responseBody = new HashMap<>();
            responseBody.put("tokens", tokens);
            when(response.getBody()).thenReturn(responseBody);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            Map<String, Map<String, String>> parsedResponseBody = underTest.readAllMaps();
            assertEquals(json, parsedResponseBody.get("tokens").get(key));
        }

        @Test
        void whenClientReturnsEmptyBody_thenReturnNull() throws CachingServiceClientException {
            Map<String, Map<String, String>> responseBody = new HashMap<>();
            when(response.getBody()).thenReturn(responseBody);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            Map<String, Map<String, String>> parsedResponseBody = underTest.readAllMaps();
            assertTrue(parsedResponseBody.isEmpty());
        }

        @Test
        void whenClientReturnsNotOk_thenThrowException() {
            when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            assertThrows(CachingServiceClientException.class, () -> underTest.readAllMaps());
        }

        @Test
        void whenResponseBodyIsNull_thenReturnNull() throws CachingServiceClientException {
            when(response.getBody()).thenReturn(null);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            Map<String, Map<String, String>> parsedResponseBody = underTest.readAllMaps();
            assertTrue(parsedResponseBody.isEmpty());
        }
    }


    @Nested
    class GivenPointLookup {

        String queryUrl = "https://localhost:10010/cachingservice/api/v1/cache-query";
        ParameterizedTypeReference<Map<String, Map<String, String>>> responseType =
            new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
            };

        @Test
        void whenTheEndpointAnswers_thenTheFoundEntriesAreReturned() {
            ResponseEntity<Map<String, Map<String, String>>> response = mock(ResponseEntity.class);
            when(response.getBody()).thenReturn(Map.of("invalidTokens", Map.of("hash", "record")));
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType))).thenReturn(response);

            var result = underTest.getMapItems(Map.of("invalidTokens", List.of("hash")));

            assertEquals("record", result.get("invalidTokens").get("hash"));
        }

        @Test
        void whenTheBodyIsNull_thenNothingWasFound() {
            ResponseEntity<Map<String, Map<String, String>>> response = mock(ResponseEntity.class);
            when(response.getBody()).thenReturn(null);
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType))).thenReturn(response);

            assertTrue(underTest.getMapItems(Map.of("invalidTokens", List.of("hash"))).isEmpty());
        }

        @Test
        void whenTheCallFails_thenItThrowsSoTheCallerCanFailClosed() {
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType)))
                .thenThrow(new RestClientException("oops"));

            assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));
        }

        /**
         * Zowe components are installed individually, so this ZAAS can be pointed at a caching service that
         * predates the endpoint. That must degrade to the slower whole-map read with a catalogued error, not
         * reject every personal access token fleet-wide.
         */
        @ParameterizedTest
        @CsvSource({"404", "405"})
        void givenACachingServiceWithoutTheEndpoint_thenTheProbeReportsItAndCataloguesTheError(int status) {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(underTest, "apimlLog", apimlLog);
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(status), "no such endpoint", null, null, null));

            assertFalse(underTest.probeMapItemQuery());
            assertFalse(underTest.supportsMapItemQuery());
            verify(apimlLog).log("org.zowe.apiml.zaas.pat.cachingServiceTooOld", CachingServiceClient.MIN_CACHING_SERVICE_VERSION);
        }

        @Test
        void givenACurrentCachingService_thenTheProbeSaysSo() {
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

            assertTrue(underTest.probeMapItemQuery());
            assertTrue(underTest.supportsMapItemQuery());
        }

        /**
         * Any status other than "no such endpoint" still proves the endpoint is there, so it must not send
         * validation down the slow path.
         */
        @Test
        void givenSomeOtherError_thenTheEndpointIsStillConsideredPresent() {
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "bad request", null, null, null));

            assertTrue(underTest.probeMapItemQuery());
        }

        @Test
        void givenTheCachingServiceIsNotRegisteredYet_thenTheProbeIsInconclusiveAndStaysOptimistic() {
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));

            assertTrue(underTest.probeMapItemQuery());
        }

        @Test
        void givenALookupThatHitsAMissingEndpoint_thenTheFallbackIsRemembered() {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(underTest, "apimlLog", apimlLog);
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "no such endpoint", null, null, null));

            assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));
            assertFalse(underTest.supportsMapItemQuery());
            verify(apimlLog).log("org.zowe.apiml.zaas.pat.cachingServiceTooOld", CachingServiceClient.MIN_CACHING_SERVICE_VERSION);
        }

        /**
         * The answer has to be re-checked, so that upgrading the caching service heals the deployment on its
         * own and so the catalogued error keeps being emitted rather than scrolling away once.
         */
        @Test
        void givenTheRecheckIntervalHasPassed_thenTheEndpointIsProbedAgain() {
            ReflectionTestUtils.setField(underTest, "mapItemQuerySupported", false);
            ReflectionTestUtils.setField(underTest, "querySupportCheckedAt", new java.util.concurrent.atomic.AtomicLong(1L));
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

            assertTrue(underTest.supportsMapItemQuery());
        }

        /**
         * With no local caching of the answer, this lookup is on every personal access token request. Once the
         * store has stopped answering, waiting out a timeout per request just ties up ZAAS threads - the answer
         * is "not valid" either way, so it is better arrived at immediately.
         */
        @Test
        void givenRepeatedFailures_thenFurtherLookupsAreNotEvenAttempted() {
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType)))
                .thenThrow(new RestClientException("the store is not answering"));
            ReflectionTestUtils.setField(underTest, "lookupRestTemplate", restTemplate);

            for (int i = 0; i < 5; i++) {
                assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));
            }
            clearInvocations(restTemplate);

            assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));

            verify(restTemplate, never()).exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType));
        }

        @Test
        void givenTheCircuitHasBeenOpenLongEnough_thenOneLookupIsLetThroughAgain() {
            ReflectionTestUtils.setField(underTest, "lookupRestTemplate", restTemplate);
            ReflectionTestUtils.setField(underTest, "lookupCircuitOpenedAt",
                new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() - 60_000L));
            ResponseEntity<Map<String, Map<String, String>>> response = mock(ResponseEntity.class);
            when(response.getBody()).thenReturn(Map.of());
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType))).thenReturn(response);

            assertTrue(underTest.getMapItems(Map.of()).isEmpty());
        }

        @Test
        void givenASuccessfulLookup_thenTheFailureCountIsReset() {
            ReflectionTestUtils.setField(underTest, "lookupRestTemplate", restTemplate);
            ResponseEntity<Map<String, Map<String, String>>> response = mock(ResponseEntity.class);
            when(response.getBody()).thenReturn(Map.of());
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType)))
                .thenThrow(new RestClientException("blip"))
                .thenThrow(new RestClientException("blip"))
                .thenReturn(response);

            assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));
            assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));
            assertTrue(underTest.getMapItems(Map.of()).isEmpty());
            assertEquals(0, ((java.util.concurrent.atomic.AtomicInteger)
                ReflectionTestUtils.getField(underTest, "consecutiveLookupFailures")).get());
        }

        /**
         * A read is idempotent and a dropped connection is the most common way for it to fail at all, so one
         * retry is worth having - the short timeout is what keeps it affordable.
         */
        @Test
        void givenATransportFailure_thenTheLookupIsRetriedOnce() {
            ReflectionTestUtils.setField(underTest, "lookupRestTemplate", restTemplate);
            ResponseEntity<Map<String, Map<String, String>>> response = mock(ResponseEntity.class);
            when(response.getBody()).thenReturn(Map.of("invalidTokens", Map.of("hash", "record")));
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType)))
                .thenThrow(new ResourceAccessException("connection reset"))
                .thenReturn(response);

            assertEquals("record", underTest.getMapItems(Map.of()).get("invalidTokens").get("hash"));
            verify(restTemplate, times(2)).exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType));
        }

        /**
         * A caching service that is too old is not a failing store: it answers perfectly promptly, just with a
         * 404. Opening the circuit for that would hide the version skew behind a different symptom.
         */
        @Test
        void givenAMissingEndpoint_thenTheCircuitStaysClosed() {
            ApimlLogger apimlLog = mock(ApimlLogger.class);
            ReflectionTestUtils.setField(underTest, "apimlLog", apimlLog);
            ReflectionTestUtils.setField(underTest, "lookupRestTemplate", restTemplate);
            when(restTemplate.exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(responseType)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "no such endpoint", null, null, null));

            for (int i = 0; i < 6; i++) {
                assertThrows(CachingServiceClientException.class, () -> underTest.getMapItems(Map.of()));
            }

            assertEquals(0, ((java.util.concurrent.atomic.AtomicInteger)
                ReflectionTestUtils.getField(underTest, "consecutiveLookupFailures")).get());
        }

        @Test
        void givenTheRecheckIntervalHasNotPassed_thenNothingIsProbed() {
            ReflectionTestUtils.setField(underTest, "mapItemQuerySupported", false);
            ReflectionTestUtils.setField(underTest, "querySupportCheckedAt",
                new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis()));

            assertFalse(underTest.supportsMapItemQuery());
            verify(restTemplate, never()).exchange(eq(queryUrl), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        }
    }

    @Nested
    class GivenLegacyListRead {

        @Test
        void thenItReadsItsOwnEndpointAndNotTheCurrentOne() {
            ParameterizedTypeReference<Map<String, Map<String, String>>> responseType =
                new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
                };
            ResponseEntity<Map<String, Map<String, String>>> response = mock(ResponseEntity.class);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            when(response.getBody()).thenReturn(Map.of("invalidTokens", Map.of("hash", "record")));
            when(restTemplate.exchange(eq("https://localhost:10010/cachingservice/api/v1/cache-list-legacy"),
                eq(HttpMethod.GET), isNull(), eq(responseType))).thenReturn(response);

            assertEquals("record", underTest.readAllLegacyMaps().get("invalidTokens").get("hash"));
        }
    }

    @Test
    void whenClientThrowsException_thenTranslateException() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class))).thenThrow(new RestClientException("error"));
        assertThrows(CachingServiceClientException.class, () -> underTest.appendList("mapKey", new CachingServiceClient.KeyValue()));
    }

    @Nested
    class GivenEvictItemTest {
        ResponseEntity<Map<String, Map<String, String>>> response;
        String urlBaseTokens;
        String urlBaseUsers;
        String urlBaseScopes;

        @BeforeEach
        void setup() {
            ParameterizedTypeReference<Map<String, Map<String, String>>> responseType =
                new ParameterizedTypeReference<Map<String, Map<String, String>>>() {
                };
            response = (ResponseEntity<Map<String, Map<String, String>>>) mock(ResponseEntity.class);

            urlBaseTokens = "https://localhost:10010/cachingservice/api/v1/cache-list/evict/tokens/invalidTokens";
            urlBaseUsers = "https://localhost:10010/cachingservice/api/v1/cache-list/evict/rules/invalidUsers";
            urlBaseScopes = "https://localhost:10010/cachingservice/api/v1/cache-list/evict/rules/invalidScopes";

            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(responseType))).thenReturn(response);
        }

        @Test
        void whenCallArePerformed_thenReturnSuccessResponse() {
            when(response.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT);
            assertDoesNotThrow(() -> underTest.evictTokens("invalidTokens"));
            verify(restTemplate).exchange(urlBaseTokens, HttpMethod.DELETE, new HttpEntity<>(null, CachingServiceClient.getDefaultHeaders()), String.class);
            assertDoesNotThrow(() -> underTest.evictRules("invalidUsers"));
            verify(restTemplate).exchange(urlBaseUsers, HttpMethod.DELETE, new HttpEntity<>(null, CachingServiceClient.getDefaultHeaders()), String.class);
            assertDoesNotThrow(() -> underTest.evictRules("invalidScopes"));
            verify(restTemplate).exchange(urlBaseScopes, HttpMethod.DELETE, new HttpEntity<>(null, CachingServiceClient.getDefaultHeaders()), String.class);
        }

        @Test
        void createWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.evictTokens("invalidTokens"));
            assertThrows(CachingServiceClientException.class, () -> underTest.evictRules("invalidScopes"));
        }

    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class CachingServiceAuthorization {

        @Mock
        private ApimlLogger apimlLogger;

        @Test
        void givenCredentials_whenSetCredentials_thenSetAuthorizationHeader() {
            var service = new CachingServiceClient(mock(RestTemplate.class), null);
            ReflectionTestUtils.setField(service, "cachingServiceUserId", "user");
            ReflectionTestUtils.setField(service, "cachingServicePassword", "password");
            ReflectionTestUtils.setField(service, "apimlLog", apimlLogger);

            service.afterPropertiesSet();

            // Verify that no warning is logged
            verify(apimlLogger, times(0)).log("org.zowe.apiml.security.common.auth.missingDefaultCredentials");
            // Verify that Basic authHeader is in the defaultHeaders map
            var headers = (MultiValueMap<String, String>) ReflectionTestUtils.getField(service, "defaultHeaders");
            assertEquals("Basic dXNlcjpwYXNzd29yZA==", headers.get(HttpHeaders.AUTHORIZATION).get(0));
        }

        @ParameterizedTest
        @CsvSource({
            ",password,,",
            "user,,",
            ",,"
        })
        void givenIncompleteCredentials_whenSetCredentials_thenDoNotSetAuthorization(String userId, String password) {
            var service = new CachingServiceClient(mock(RestTemplate.class), null);
            ReflectionTestUtils.setField(service, "cachingServiceUserId", userId);
            ReflectionTestUtils.setField(service, "cachingServicePassword", password);
            ReflectionTestUtils.setField(service, "apimlLog", apimlLogger);

            service.afterPropertiesSet();

            // Verify that warning is logged
            verify(apimlLogger, times(1)).log("org.zowe.apiml.security.common.auth.missingDefaultCredentials");
            // Verify that Basic authHeader is not in the defaultHeaders map
            var headers = (MultiValueMap<String, String>) ReflectionTestUtils.getField(service, "defaultHeaders");
            assertNull(headers.get(HttpHeaders.AUTHORIZATION));
        }

    }

}
