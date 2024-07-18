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
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

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
                void andError_thenError() throws JsonProcessingException {
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
                void andSuccess_thenSuccessResult() {

                }

                @Test
                void andError_thenErrorResult() {

                }
            }

            @Nested
            class WhenUpdate {

                @Test
                void andSuccess_thenSuccessResult() {

                }

                @Test
                void andError_thenErrorResult() {

                }

            }

            @Nested
            class WhenRetrieve {

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

                    StepVerifier.create(loadBalancerCache.store("anuser", "aserviceid", null))
                        .expectComplete()
                        .verifyThenAssertThat();
                }

            }

            @Nested
            class WhenDelete {

            }

            @Nested
            class WhenUpdate {

            }

            @Nested
            class WhenRetrieve {

            }

        }

    }

}
