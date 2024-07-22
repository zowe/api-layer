/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.loadbalancer;

import io.jsonwebtoken.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class DeterministicLoadBalancerTest {

    private static final String VALID_TOKEN = "eyJraWQiOiJvekdfeVNNSFJzVlFGbU4xbVZCZVMtV3RDdXBZMXItSzdld2JlbjA5SUJnIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJ1dWlkIjoiNjQ1Yjg5NDYtOWY1NC00NmY4LWJiMDMtNDFiMjM5NTg2OWExIiwic3ViIjoiVVNFUiIsImlzcyI6InpPU01GIiwiaWF0IjoxNzIxNTUyNzUyLCJleHAiOjE3MjE1NTQ1NTJ9.CQyz5mRCntZ1W63yquKw-1pZAjeUBrlm_27-gtMJkeIhUsnqOJng2lmjWj1pa1H7D_uyS-suXoYu5t2JttrBNHm8BGJdigdD5J0GoGepMtfKXbORNhxf0S3U-lTbeKxSTQXcV5GUp05A3GaKdE65KOogOvaJBM79a5K7NJsIPAPgz03kJ3JOZUsCGW8NOrKcnujgyxJ9c4wGnvOlCtxItM-dlcIgz--iJVEynQ-RB0lICYCjQ_R_C7ND0tMo-o5UGCN6GxTOO16N5TpH47Krd46Z43skmN3sqGhk0jQ3kIVemeF434i3gYUjV42nOEEvysWyL12LiKqQFsHVbZMA8w";
    private static final int DEFAULT_EXPIRATION_HS = 1;

    @Mock
    private ServiceInstanceListSupplier delegate;

    @Mock
    private LoadBalancerCache lbCache;

    @Mock
    private LoadBalancerClientFactory factory;

    @SuppressWarnings("rawtypes")
    @Mock
    private Request request;

    @Mock
    private ServiceInstance instance1;

    @Mock
    private ServiceInstance instance2;

    @Mock
    private Clock clock;

    private DeterministicLoadBalancer loadBalancer;

    private final List<ServiceInstance> DEFAULT_LIST = new ArrayList<>();

    @BeforeEach
    void setUp() {
        DEFAULT_LIST.clear();
        DEFAULT_LIST.add(instance1);
        DEFAULT_LIST.add(instance2);

        lenient().when(instance1.getInstanceId()).thenReturn("instance1");
        lenient().when(instance1.getServiceId()).thenReturn("service");
        lenient().when(instance2.getInstanceId()).thenReturn("instance2");
        lenient().when(instance2.getServiceId()).thenReturn("service");

        var properties = new LoadBalancerProperties();
        when(factory.getProperties(any())).thenReturn(properties);
        when(delegate.getServiceId()).thenReturn("service");
        when(delegate.get(request)).thenReturn(Flux.just(DEFAULT_LIST));
        this.loadBalancer = new DeterministicLoadBalancer(delegate, factory, lbCache, clock, DEFAULT_EXPIRATION_HS);
    }

    @Nested
    class GivenDeterministicLoadBalancer {

        @Test
        void whenNoToken_thenUseDefaultList() {
            when(request.getContext()).thenReturn(null);

            StepVerifier.create(loadBalancer.get(request))
                .assertNext(chosenInstances -> {
                    assertNotNull(chosenInstances);
                    assertEquals(2, chosenInstances.size());
                })
                .expectComplete()
                .verify();
        }

        @Nested
        class GivenTokenExists {

            @Test
            void whenTokenIsInvalid_thenUseDefaultList() {
                var requestData = mock(RequestData.class);
                var context = new RequestDataContext(requestData);

                MultiValueMap<String, String> cookie = new LinkedMultiValueMap<>();
                cookie.add("apimlAuthenticationToken", "invalidToken");

                when(requestData.getCookies()).thenReturn(cookie);

                when(request.getContext()).thenReturn(context);

                StepVerifier.create(loadBalancer.get(request))
                    .assertNext(chosenInstances -> {
                        assertNotNull(chosenInstances);
                        assertEquals(2, chosenInstances.size());
                    })
                    .expectComplete()
                    .verify();
            }

            @Nested
            class GivenTokenIsValid {

                @BeforeEach
                void setUp() {
                    var requestData = mock(RequestData.class);
                    var context = new RequestDataContext(requestData);

                    MultiValueMap<String, String> cookie = new LinkedMultiValueMap<>();
                    cookie.add("apimlAuthenticationToken", VALID_TOKEN);

                    when(requestData.getCookies()).thenReturn(cookie);
                    when(request.getContext()).thenReturn(context);
                    when(clock.now()).thenReturn(Date.from(Instant.ofEpochSecond(1721552753)));
                }

                @Test
                void whenServiceDoesNotHaveMetadata_thenUseDefaultList() {
                    when(instance1.getMetadata()).thenReturn(null);
                    when(lbCache.retrieve("USER", "service")).thenReturn(Mono.just(LoadBalancerCacheRecord.NONE));

                    StepVerifier.create(loadBalancer.get(request))
                        .assertNext(chosenInstances -> {
                            assertNotNull(chosenInstances);
                            assertEquals(2, chosenInstances.size());
                        })
                        .expectComplete()
                        .verify();
                }

                @Test
                void whenServiceDoesNotUseSticky_thenUseDefaultList() {
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("apiml.lb.type", "somethingelse");
                    when(instance1.getMetadata()).thenReturn(metadata);

                    when(lbCache.retrieve("USER", "service")).thenReturn(Mono.just(LoadBalancerCacheRecord.NONE));

                    StepVerifier.create(loadBalancer.get(request))
                        .assertNext(chosenInstances -> {
                            assertNotNull(chosenInstances);
                            assertEquals(2, chosenInstances.size());
                        })
                        .expectComplete()
                        .verify();
                }

                @Nested
                class GivenServiceUsesSticky {

                    @BeforeEach
                    void setUp() {
                        Map<String, String> metadata = new HashMap<>();
                        metadata.put("apiml.lb.type", "authentication");
                        when(instance1.getMetadata()).thenReturn(metadata);
                    }

                    @Nested
                    class GivenCacheHasPreference {

                        @Test
                        void whenInstanceExists_thenUpdateList() {
                            when(lbCache.retrieve("USER", "service")).thenReturn(Mono.just(new LoadBalancerCacheRecord("instance1")));
                            when(lbCache.store(eq("USER"), eq("service"), argThat(record -> record.getInstanceId().equals("instance1"))))
                                .thenReturn(Mono.empty());

                            StepVerifier.create(loadBalancer.get(request))
                                .assertNext(chosenInstances -> {
                                    assertNotNull(chosenInstances);
                                    assertEquals(1, chosenInstances.size());
                                    assertEquals("instance1", chosenInstances.get(0).getInstanceId());
                                })
                                .expectComplete()
                                .verify();
                        }

                        @Test
                        void whenInstanceDoesNotExist_thenUpdatePreference() {
                            when(lbCache.retrieve("USER", "service")).thenReturn(Mono.just(new LoadBalancerCacheRecord("instance3")));
                            when(lbCache.store(eq("USER"), eq("service"), argThat(record -> record.getInstanceId().equals("instance1"))))
                                .thenReturn(Mono.empty());

                            StepVerifier.create(loadBalancer.get(request))
                            .assertNext(chosenInstances -> {
                                assertNotNull(chosenInstances);
                                assertEquals(1, chosenInstances.size());
                                assertEquals("instance1", chosenInstances.get(0).getInstanceId());
                            })
                            .expectComplete()
                            .verify();
                        }

                        @Test
                        void whenCacheEntryExpired_thenUpdatePreference() {
                            when(lbCache.retrieve("USER", "service")).thenReturn(Mono.just(new LoadBalancerCacheRecord("instance2", LocalDateTime.of(2023, 2, 20, 2, 2))));
                            when(lbCache.delete("USER", "service")).thenReturn(Mono.empty());
                            when(lbCache.store(eq("USER"), eq("service"), argThat(record -> record.getInstanceId().equals("instance1"))))
                                .thenReturn(Mono.empty());

                            StepVerifier.create(loadBalancer.get(request))
                            .assertNext(chosenInstances -> {
                                assertNotNull(chosenInstances);
                                assertEquals(1, chosenInstances.size());
                                assertEquals("instance1", chosenInstances.get(0).getInstanceId());
                            })
                            .expectComplete()
                            .verify();
                        }

                    }

                    @Test
                    void whenNoPreferece_thenCreateOne() {
                        when(lbCache.retrieve("USER", "service")).thenReturn(Mono.just(LoadBalancerCacheRecord.NONE));

                        when(lbCache.store(eq("USER"), eq("service"), argThat(record -> record.getInstanceId().equals("instance1"))))
                                .thenReturn(Mono.empty());

                            StepVerifier.create(loadBalancer.get(request))
                            .assertNext(chosenInstances -> {
                                assertNotNull(chosenInstances);
                                assertEquals(1, chosenInstances.size());
                                assertEquals("instance1", chosenInstances.get(0).getInstanceId());
                            })
                            .expectComplete()
                            .verify();
                    }

                }

            }

        }

        /**
         *
         *
         */
        @Nested
        class GivenStickyBalancerIgnored {

            @Nested
            class GivenInstanceIdHeaderIsPresent {

                @Mock
                private RequestData requestData;

                @BeforeEach
                void setUp() {
                    var context = new RequestDataContext(requestData);
                    MultiValueMap<String, String> cookie = new LinkedMultiValueMap<>();
                    cookie.add("apimlAuthenticationToken", "invalidToken");

                    when(request.getContext()).thenReturn(context);
                    when(requestData.getCookies()).thenReturn(cookie);
                }

                @Test
                void whenInstanceIdExists_thenChoseeIt() {
                    var headers = new HttpHeaders();
                    headers.add("X-InstanceId", "instance2");
                    when(requestData.getHeaders()).thenReturn(headers);

                    StepVerifier.create(loadBalancer.get(request))
                        .assertNext(chosenInstances -> {
                            assertNotNull(chosenInstances);
                            assertEquals(1, chosenInstances.size());
                            assertEquals("instance2", chosenInstances.get(0).getInstanceId());
                        })
                        .expectComplete()
                        .verify();
                }

                @Test
                void whenInstanceIdDoesNotExist_thenFail() {
                    var headers = new HttpHeaders();
                    headers.add("X-InstanceId", "instance3");
                    when(requestData.getHeaders()).thenReturn(headers);

                    StepVerifier.create(loadBalancer.get(request))
                        .expectErrorMatches(e ->
                            e instanceof ResponseStatusException ex && ex.getStatusCode().value() == 404 && ex.getReason().equals("Service instance not found for the provided instance ID"))
                        .verify();
                }

            }

        }

    }

}
