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

import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

// TODO refactor this separately, out of Gateway?
class CachingServiceClientTest {
    CachingServiceClient underTest;
    RestTemplate restTemplate = mock(RestTemplate.class);
    String urlBase = "https://localhost:10010/cachingservice/api/v1/cache";

    @BeforeEach
    void setUp() {
        underTest = new CachingServiceClient(restTemplate);
    }

    @Nested
    class givenCreateOperation {

        @Test
        void createWithoutProblem() {
            CachingServiceClient.KeyValue kv = new CachingServiceClient.KeyValue("Britney", "Spears");
            assertDoesNotThrow(() -> underTest.create(kv));
            verify(restTemplate).exchange(eq(urlBase), eq(HttpMethod.POST), eq(new HttpEntity<>(kv, new HttpHeaders())), eq(String.class));
        }

        @Test
        void createWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClient.CachingServiceClientException.class,() -> underTest.create(new CachingServiceClient.KeyValue("Britney", "Spears")));
        }
    }

    @Nested
    class givenUpdateOperation {
        @Test
        void updateWithoutProblem() {
            CachingServiceClient.KeyValue kv = new CachingServiceClient.KeyValue("Britney", "Speeeeers");
            assertDoesNotThrow(() -> underTest.update(kv));
            verify(restTemplate).exchange(eq(urlBase), eq(HttpMethod.PUT), eq(new HttpEntity<>(kv, new HttpHeaders())), eq(String.class));
        }

        @Test
        void updateWithExceptionFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClient.CachingServiceClientException.class,() -> underTest.update(new CachingServiceClient.KeyValue("Britney", "Spears")));
        }
    }

    @Nested
    class givenReadOperation {

        private String keyToRead = "reee";

        @Test
        void readWithNullResponseOrNullBody() {
            assertThrows(CachingServiceClient.CachingServiceClientException.class, () -> underTest.read(keyToRead));
            verify(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            ResponseEntity<CachingServiceClient.KeyValue> responseEntity = mock(ResponseEntity.class);
            doReturn(false).when(responseEntity).hasBody();
            doReturn(responseEntity).when(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            assertThrows(CachingServiceClient.CachingServiceClientException.class, () -> underTest.read(keyToRead));
        }

        @Test
        void readWithoutProblem() throws CachingServiceClient.CachingServiceClientException {
            ResponseEntity<CachingServiceClient.KeyValue> responseEntity = mock(ResponseEntity.class);
            doReturn(true).when(responseEntity).hasBody();
            doReturn(new CachingServiceClient.KeyValue(keyToRead, "Wonder")).when(responseEntity).getBody();
            doReturn(responseEntity).when(restTemplate).exchange(eq(urlBase + "/" + keyToRead), eq(HttpMethod.GET), any(HttpEntity.class), eq(CachingServiceClient.KeyValue.class));
            assertThat(underTest.read(keyToRead).getValue(), is("Wonder"));
        }

        @Test
        void readWithExceptonFromRestTemplateThrowsDefined() {
            doThrow(new RestClientException("oops")).when(restTemplate).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
            assertThrows(CachingServiceClient.CachingServiceClientException.class, () -> underTest.read(keyToRead));
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
            assertThrows(CachingServiceClient.CachingServiceClientException.class,() -> underTest.delete(keyToDelete));
        }
    }


}
