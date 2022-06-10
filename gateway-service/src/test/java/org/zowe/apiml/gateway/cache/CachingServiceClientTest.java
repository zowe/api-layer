/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.token.ApimlAccessTokenProvider;

import java.util.HashMap;
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
        underTest = new CachingServiceClient(restTemplate, "https://localhost:10010");
    }

    @Nested
    class givenCreateOperation {

        @Test
        void createWithoutProblem() {
            CachingServiceClient.KeyValue kv = new CachingServiceClient.KeyValue("Britney", "Spears");
            assertDoesNotThrow(() -> underTest.create(kv));
            verify(restTemplate).exchange(urlBase, HttpMethod.POST, new HttpEntity<>(kv, new HttpHeaders()), String.class);
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
            verify(restTemplate).exchange(urlBase, HttpMethod.PUT, new HttpEntity<>(kv, new HttpHeaders()), String.class);
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
            assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
            verify(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            ResponseEntity<CachingServiceClient.KeyValue> responseEntity = mock(ResponseEntity.class);
            doReturn(false).when(responseEntity).hasBody();
            doReturn(responseEntity).when(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
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
        void readWithExceptonFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClientException.class, () -> underTest.read(keyToRead));
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
        ResponseEntity<Map<String, String>> response;

        @BeforeEach
        void setup() {
            ParameterizedTypeReference<Map<String, String>> responseType =
                new ParameterizedTypeReference<Map<String, String>>() {
                };
            response = (ResponseEntity<Map<String, String>>) mock(ResponseEntity.class);
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(responseType))).thenReturn(response);
        }

        @Test
        void whenClientReturnsBody_thenParseTheResponse() throws CachingServiceClientException, JsonProcessingException {
            String key = "token";
            ApimlAccessTokenProvider.AccessTokenContainer container = new ApimlAccessTokenProvider.AccessTokenContainer(null, key, null, null, null, null);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> responseBody = new HashMap<>();
            String json = mapper.writeValueAsString(container);
            responseBody.put(key, json);
            when(response.getBody()).thenReturn(responseBody);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            Map<String, String> parsedResponseBody = underTest.readInvalidatedTokens();
            assertEquals(json, parsedResponseBody.get(key));
        }

        @Test
        void whenClientReturnsEmptyBody_thenReturnNull() throws CachingServiceClientException {
            Map<String, String> responseBody = new HashMap<>();
            when(response.getBody()).thenReturn(responseBody);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            Map<String, String> parsedResponseBody = underTest.readInvalidatedTokens();
            assertNull(parsedResponseBody);
        }

        @Test
        void whenClientReturnsNotOk_thenThrowException() {
            when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            assertThrows(CachingServiceClientException.class, () -> underTest.readInvalidatedTokens());
        }

        @Test
        void whenResponseBodyIsNull_thenReturnNull() throws CachingServiceClientException {
            when(response.getBody()).thenReturn(null);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            Map<String, String> parsedResponseBody = underTest.readInvalidatedTokens();
            assertNull(parsedResponseBody);
        }
    }

    @Test
    void whenClientThrowsException_thenTranslateException() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class))).thenThrow(new RestClientException("error"));
        assertThrows(CachingServiceClientException.class, () -> underTest.appendList(new CachingServiceClient.KeyValue()));
    }


}
