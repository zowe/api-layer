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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.caching.CachingServiceClient.KeyValue;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@ExtendWith(MockitoExtension.class)
class LoadBalancerCacheTest {

    @Mock
    private CachingServiceClient cachingServiceClient;

    @Mock
    private Map<String, LoadBalancerCacheRecord> map;

    private LoadBalancerCache loadBalancerCache;

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        loadBalancerCache = new LoadBalancerCache(cachingServiceClient);
        ReflectionTestUtils.setField(loadBalancerCache, "localCache", map);
    }

    @Nested
    class GivenLoadBalancerCache {

        @Nested
        class GivenRemoteCacheExists {

            @AfterEach
            void onFinish() {
                verifyNoInteractions(map);
            }

            @Nested
            class WhenCreate {

                @Test
                void andSuccess_thenSuccess() throws JsonProcessingException {
                    var record = new LoadBalancerCacheRecord("instance1");
                    when(cachingServiceClient.create(new KeyValue("lb.anuser:aserviceid", mapper.writeValueAsString(record))))
                        .thenReturn(empty());

                    StepVerifier.create(loadBalancerCache.store("anuser", "aserviceid", record))
                        .expectComplete()
                        .verifyThenAssertThat();
                }

                @Test
                void andGenericError_thenError() throws JsonProcessingException {
                    var record = new LoadBalancerCacheRecord("instance1");
                    when(cachingServiceClient.create(new KeyValue("lb.anuser:aserviceid", mapper.writeValueAsString(record))))
                        .thenReturn(error(new CachingServiceClientException(500, "error")));

                        StepVerifier.create(loadBalancerCache.store("anuser", "aserviceid", record))
                            .expectErrorMatches(exception -> exception.getMessage().equals("error"))
                            .verify();
                }

                @Test
                void andErrorCacheConflict_thenError() throws JsonProcessingException {
                    var record = new LoadBalancerCacheRecord("instance1");
                    var keyValue = new KeyValue("lb.anuser:aserviceid", mapper.writeValueAsString(record));
                    when(cachingServiceClient.create(keyValue))
                        .thenReturn(error(new CachingServiceClientException(409, "error")));

                    when(cachingServiceClient.update(keyValue)).thenReturn(empty());

                    StepVerifier.create(loadBalancerCache.store("anuser", "aserviceid", record))
                        .expectComplete()
                        .verify();
                }

            }

            @Nested
            class WhenDelete {

                @Test
                void andSuccess_thenSuccessResult() throws JsonProcessingException {
                    when(cachingServiceClient.delete("lb.anuser:aserviceid"))
                        .thenReturn(empty());

                    StepVerifier.create(loadBalancerCache.delete("anuser", "aserviceid"))
                        .expectComplete()
                        .verifyThenAssertThat();
                }

                @Test
                void andGenericError_thenErrorResult() {
                    when(cachingServiceClient.delete("lb.anuser:aserviceid"))
                        .thenReturn(error(new CachingServiceClientException(500, "error")));

                        StepVerifier.create(loadBalancerCache.delete("anuser", "aserviceid"))
                            .expectErrorMatches(exception -> exception.getMessage().equals("error"))
                            .verify();
                }
            }

            @Nested
            class WhenRetrieve {

                @Test
                void andSuccess_thenReturnValue() throws JsonProcessingException {
                    var key = "lb.anuser:aserviceid";
                    var record = new LoadBalancerCacheRecord("instanceId");
                    var keyValue = new KeyValue(key, mapper.writeValueAsString(record));
                    when(cachingServiceClient.read(key)).thenReturn(just(keyValue));

                    StepVerifier.create(loadBalancerCache.retrieve("anuser", "aserviceid"))
                        .expectNext(record)
                        .verifyComplete();
                }

                @Test
                void andNotFound_thenReturnEmpty() throws JsonProcessingException {
                    var key = "lb.anuser:aserviceid";
                    when(cachingServiceClient.read(key)).thenReturn(empty());

                    StepVerifier.create(loadBalancerCache.retrieve("anuser", "aserviceid"))
                        .expectComplete()
                        .verify();
                }

                @Test
                void andGenericError_thenReturnError() {
                    var key = "lb.anuser:aserviceid";
                    when(cachingServiceClient.read(key)).thenReturn(Mono.error(new CachingServiceClientException(500, "error")));

                    StepVerifier.create(loadBalancerCache.retrieve("anuser", "aserviceid"))
                        .expectErrorMatches(e -> e instanceof CachingServiceClientException exception && exception.getMessage().equals("error") && exception.getStatusCode() == 500)
                        .verify();
                }

            }

        }

        @Nested
        class GivenRemoveCacheDoesNotExist {

            @BeforeEach
            void setUp() {
                ReflectionTestUtils.setField(loadBalancerCache, "remoteCache", null);
            }

            @Nested
            class WhenCreate {

                @Test
                void andSuccess_thenSuccess() {
                    var record = new LoadBalancerCacheRecord("instance1");
                    when(map.put("lb.anuser:aserviceid", record)).thenReturn(record);
                    loadBalancerCache.store("anuser", "aserviceid", record);
                    verifyNoInteractions(cachingServiceClient);
                }

            }

            @Nested
            class WhenDelete {

                @Test
                void andSuccess_thenSuccess() {
                    var key = "lb.anuser:aserviceid";
                    when(map.remove(key)).thenReturn(null);
                    loadBalancerCache.delete("anuser", "aserviceid");
                    verifyNoInteractions(cachingServiceClient);
                }

            }

            @Nested
            class WhenRetrieve {

                @Test
                void andSuccess_thenSuccess() {
                    var record = new LoadBalancerCacheRecord("instance1");
                    var key = "lb.anuser:aserviceid";
                    when(map.get(key)).thenReturn(record);
                    assertEquals(record, loadBalancerCache.retrieve("anuser", "aserviceid").block());
                    verifyNoInteractions(cachingServiceClient);
                }

                @Test
                void andNotFound_thenEmpty() {
                    var key = "lb.anuser:aserviceid";
                    when(map.get(key)).thenReturn(null);
                    var result = loadBalancerCache.retrieve("anuser", "aserviceid");
                    assertNotNull(result);
                    assertEquals(Mono.empty(), result);
                    verifyNoInteractions(cachingServiceClient);
                }

            }

        }

    }

}
