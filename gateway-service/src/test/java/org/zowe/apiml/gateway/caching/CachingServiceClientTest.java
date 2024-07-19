/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.gateway.caching.CachingServiceClient.KeyValue;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

@ExtendWith(MockitoExtension.class)
class CachingServiceClientTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private ClientResponse clientResponse;

    private CachingServiceClient client;
    private WebClient webClient;

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp () {
        webClient = spy(WebClient.builder().exchangeFunction(exchangeFunction).build());
        client = new CachingServiceClient(webClient);
        lenient().when(clientResponse.releaseBody()).thenReturn(empty());
    }

    @Nested
    class GivenCachingServiceClient {

        private void mockResponse(int statusCode) {
            when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(just(clientResponse));
            when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(statusCode));
        }

        private Predicate<Throwable> assertCachingServiceClientException(int statusCode) {
            return e -> e instanceof CachingServiceClientException ex && ex.getMessage().contains(String.valueOf(statusCode));
        }

        @Nested
        class WhenCreate {

            @Test
            void andServerSuccess_thenSuccess() throws JsonProcessingException {
                var record = new LoadBalancerCacheRecord("instanceId");
                var kv = new KeyValue("lb.anuser:aservice", mapper.writeValueAsString(record));

                mockResponse(200);

                StepVerifier.create(client.create(kv))
                    .expectComplete()
                    .verify();
            }

            @Test
            void andServerError_thenError() throws JsonProcessingException {
                var record = new LoadBalancerCacheRecord("instanceId");
                var kv = new KeyValue("lb.anuser:aservice", mapper.writeValueAsString(record));

                mockResponse(500);

                StepVerifier.create(client.create(kv))
                    .verifyErrorMatches(assertCachingServiceClientException(500));
            }

            @Test
            void andClientError_thenError() throws JsonProcessingException {
                var record = new LoadBalancerCacheRecord("instanceId");
                var kv = new KeyValue("lb.anuser:aservice", mapper.writeValueAsString(record));

                mockResponse(404);

                StepVerifier.create(client.create(kv))
                    .verifyErrorMatches(assertCachingServiceClientException(404));
            }

        }

        @Nested
        class WhenDelete {

            @Test
            void andServerSuccess_thenSuccessAndContent() {
                var key = "key1";
                mockResponse(200);

                StepVerifier.create(client.delete(key))
                    .expectComplete()
                    .verify();

            }

            @Test
            void andServerError_thenError() {
                mockResponse(500);

                StepVerifier.create(client.delete("key2"))
                    .verifyErrorMatches(assertCachingServiceClientException(500));
            }

            @Test
            void andClientError_thenError() {
                mockResponse(404);

                StepVerifier.create(client.delete("key2"))
                    .verifyErrorMatches(assertCachingServiceClientException(404));
            }

        }

        @Nested
        class WhenRead {

            @Test
            void andServerSuccess_thenSuccessAndContent() {
                mockResponse(200);
                var kv = new KeyValue("key", "value");
                when(clientResponse.bodyToMono(KeyValue.class)).thenReturn(just(kv));

                StepVerifier.create(client.read("key"))
                    .expectNext(kv)
                    .verifyComplete();
            }

            @Test
            void andServerError_thenError() {
                mockResponse(500);

                StepVerifier.create(client.read("key"))
                    .verifyErrorMatches(assertCachingServiceClientException(500));
            }

            @Test
            void andNotFound_thenEmpty() {
                mockResponse(404);

                StepVerifier.create(client.read("key"))
                    .expectComplete()
                    .verify();
            }

            @Test
            void andOtherClientErorr_thenEmpty() {
                mockResponse(400);

                StepVerifier.create(client.read("key"))
                    .expectComplete()
                    .verify();
            }

        }

        @Nested
        class WhenUpdate {

            @Test
            void andServerSuccess_thenSucess() {
                mockResponse(200);
                var kv = new KeyValue("key", "value");

                StepVerifier.create(client.update(kv))
                    .expectComplete()
                    .verify();
            }

            @Test
            void andServerError_thenError() {
                mockResponse(500);
                var kv = new KeyValue("key", "value");

                StepVerifier.create(client.update(kv))
                    .verifyErrorMatches(assertCachingServiceClientException(500));
            }

            @Test
            void andClientError_thenError() {
                mockResponse(404);
                var kv = new KeyValue("key", "value");

                StepVerifier.create(client.update(kv))
                    .verifyErrorMatches(assertCachingServiceClientException(404));
            }

        }

    }

}
