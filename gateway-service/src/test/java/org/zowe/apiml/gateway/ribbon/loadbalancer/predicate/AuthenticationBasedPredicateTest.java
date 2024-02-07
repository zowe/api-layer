/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer.predicate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.*;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.loadbalancer.LoadBalancingContext;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.RequestAuthenticationService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class AuthenticationBasedPredicateTest {
    String SERVICE_ID = "serviceID";
    String VALID_USER = "annie";
    String VALID_INSTANCE = "fox_jackal";

    RequestAuthenticationService authenticationService;
    LoadBalancerCache cache;
    AuthenticationBasedPredicate underTest;
    RequestContext requestContext;
    LoadBalancingContext context;

    @BeforeEach
    void setUp() {
        authenticationService = mock(RequestAuthenticationService.class);
        cache = mock(LoadBalancerCache.class);

        context = mock(LoadBalancingContext.class);
        requestContext = mock(RequestContext.class);
        when(context.getRequestContext()).thenReturn(requestContext);
        InstanceInfo info = mock(InstanceInfo.class);
        when(info.getInstanceId()).thenReturn("hostname:service:port");
        when(context.getInstanceInfo()).thenReturn(info);

        underTest = new AuthenticationBasedPredicate(authenticationService, cache, 8);
    }

    @Test
    void testToString() {
        assertThat(underTest.toString(), is("AuthenticationBasedPredicate (USERNAME)"));
    }

    @Nested
    class WhenFiltering {
        @Nested
        class GivenNoService {
            @Test
            void returnTrue() {
                when(requestContext.get(SERVICE_ID_KEY)).thenReturn(null);

                boolean amongSelected = underTest.apply(context, mock(DiscoveryEnabledServer.class));
                assertThat(amongSelected, is(true));
            }
        }

        @Nested
        class GivenValidService {
            @BeforeEach
            void setUp() {
                when(requestContext.get(SERVICE_ID_KEY)).thenReturn(SERVICE_ID);
            }

            @Nested
            class AndNoUser {
                @Test
                void returnTrue() {
                    when(authenticationService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

                    boolean amongSelected = underTest.apply(context, mock(DiscoveryEnabledServer.class));
                    assertThat(amongSelected, is(true));
                }
            }

            @Nested
            class AndValidUser {
                @BeforeEach
                void setUp() {
                    when(authenticationService.getPrincipalFromRequest(any())).thenReturn(Optional.of(VALID_USER));
                }

                @Test
                void withoutCachedValue_returnTrue() {
                    when(cache.retrieve(VALID_USER, SERVICE_ID)).thenReturn(null);

                    boolean amongSelected = underTest.apply(context, mock(DiscoveryEnabledServer.class));
                    assertThat(amongSelected, is(true));
                }

                @Nested
                class AndCachedValue {
                    @BeforeEach
                    void setUp() {
                        when(cache.retrieve(VALID_USER, SERVICE_ID)).thenReturn(new LoadBalancerCacheRecord(VALID_INSTANCE));
                    }

                    @Test
                    void withTheSameInstanceId_returnTrue() {
                        DiscoveryEnabledServer server = discoveryEnabledServer(VALID_INSTANCE);

                        boolean amongSelected = underTest.apply(context, server);
                        assertThat(amongSelected, is(true));
                    }

                    @Test
                    void withInstanceIdNull_returnTrue() {
                        when(cache.retrieve(VALID_USER, SERVICE_ID)).thenReturn(new LoadBalancerCacheRecord(null));
                        DiscoveryEnabledServer server = discoveryEnabledServer(VALID_INSTANCE);

                        boolean amongSelected = underTest.apply(context, server);
                        assertThat(amongSelected, is(true));
                    }

                    @Test
                    void withDifferentInstanceId_returnFalse() {
                        DiscoveryEnabledServer server = discoveryEnabledServer("invalid-fox");

                        boolean amongSelected = underTest.apply(context, server);
                        assertThat(amongSelected, is(false));
                    }
                }

                @Nested
                class AndOldCachedValue {
                    @BeforeEach
                    void setUp() {
                        when(cache.retrieve(VALID_USER, SERVICE_ID)).thenReturn(
                            new LoadBalancerCacheRecord(VALID_INSTANCE, LocalDateTime.now().minus(10, ChronoUnit.HOURS))
                        );
                    }

                    @Test
                    void withDifferentInstanceId_returnTrue() {
                        DiscoveryEnabledServer server = discoveryEnabledServer("invalid-fox");

                        boolean amongSelected = underTest.apply(context, server);
                        assertThat(amongSelected, is(true));
                    }
                }
            }
        }
    }

    private DiscoveryEnabledServer discoveryEnabledServer(String instanceId) {
        DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
        InstanceInfo info = mock(InstanceInfo.class);
        when(server.getInstanceInfo()).thenReturn(info);
        when(info.getInstanceId()).thenReturn(instanceId);
        return server;
    }
}
