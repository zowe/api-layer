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
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.token.ApimlAccessTokenProvider;

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
        ResponseEntity<String[]> response;

        @BeforeEach
        void setup() {
            response = (ResponseEntity<String[]>) mock(ResponseEntity.class);
            when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(), eq(String[].class))).thenReturn(response);
        }

        @Test
        void whenClientReturnsBody_thenParseTheResponse() throws CachingServiceClientException, JsonProcessingException {
            ApimlAccessTokenProvider.AccessTokenContainer container = new ApimlAccessTokenProvider.AccessTokenContainer(null, "token", null, null, null, null);
            ObjectMapper mapper = new ObjectMapper();
            String[] responseBody = new String[]{mapper.writeValueAsString(container)};
            when(response.getBody()).thenReturn(responseBody);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            ApimlAccessTokenProvider.AccessTokenContainer[] parsedResponseBody = underTest.readList("key");
            assertEquals(container.getTokenValue(), parsedResponseBody[0].getTokenValue());
        }

        @Test
        void whenClientReturnsEmptyBody_thenReturnNull() throws CachingServiceClientException {
            String[] responseBody = new String[]{};
            when(response.getBody()).thenReturn(responseBody);
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);
            ApimlAccessTokenProvider.AccessTokenContainer[] parsedResponseBody = underTest.readList("key");
            assertNull(parsedResponseBody);
        }

        @Test
        void whenClientReturnsNotOk_thenThrowException() {
            when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
            assertThrows(CachingServiceClientException.class, () -> underTest.readList("key"));
        }
    }

    @Test
    void whenClientThrowsException_thenTranslateException() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class))).thenThrow(new RestClientException("error"));
        assertThrows(CachingServiceClientException.class, () -> underTest.appendList(new CachingServiceClient.KeyValue()));
    }


}
