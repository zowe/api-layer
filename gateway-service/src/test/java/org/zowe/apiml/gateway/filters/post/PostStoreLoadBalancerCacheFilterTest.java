/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.post;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.RequestAuthenticationService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

class PostStoreLoadBalancerCacheFilterTest {

    private PostStoreLoadBalancerCacheFilter underTest;

    private RequestAuthenticationService authenticationService;
    private LoadBalancerCache loadBalancerCache;

    private InstanceInfo info;

    private final String VALID_USER = "annie";
    private final String VALID_SERVICE_ID = "hairdresser-service";
    private final String VALID_INSTANCE_ID = "kamenicka";

    @BeforeEach
    void setUp() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(SERVICE_ID_KEY, VALID_SERVICE_ID);

        info = mock(InstanceInfo.class);
        RequestContextUtils.setInstanceInfo(info);

        authenticationService = mock(RequestAuthenticationService.class);
        loadBalancerCache = new LoadBalancerCache(null);

        underTest = new PostStoreLoadBalancerCacheFilter(authenticationService, loadBalancerCache);
    }

    @Test
    void verifyFilterProperties() {
        assertThat(underTest.filterOrder(), is(SEND_RESPONSE_FILTER_ORDER - 1));
        assertThat(underTest.filterType(), is(POST_TYPE));
    }

    @Nested
    class GivenAuthenticationBasedBalancingIsntEnabled {
        @BeforeEach
        void setUp() {
            Map<String, String> metadata = new HashMap<>();
            when(info.getMetadata()).thenReturn(metadata);
        }

        @Nested
        class GivenAuthenticationAndInstanceInfo {

            @Test
            void whenShouldFilterIsFalse_thenDontAddToCache() {
                when(info.getInstanceId()).thenReturn(VALID_INSTANCE_ID);
                assertThat(underTest.shouldFilter(), is(false));
                assertThat(loadBalancerCache.retrieve(VALID_USER, VALID_SERVICE_ID), is(nullValue()));
            }
        }
    }

    @Nested
    class GivenAuthenticationBasedBalancingIsEnabled {
        @BeforeEach
        void setUp() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("apiml.lb.type", "authentication");
            when(info.getMetadata()).thenReturn(metadata);
        }

        @Nested
        class GivenAuthenticationAndInstanceInfo {

            @Test
            void whenNotInCacheAddInstanceToCache() {
                when(info.getInstanceId()).thenReturn(VALID_INSTANCE_ID);

                when(authenticationService.getPrincipalFromRequest(any())).thenReturn(Optional.of(VALID_USER));
                assertThat(underTest.shouldFilter(), is(true));
                underTest.run();
                assertThat(loadBalancerCache.retrieve(VALID_USER, VALID_SERVICE_ID), is(not(nullValue())));
            }

            @Test
            void whenInCacheDoNothing() {
                loadBalancerCache.store(VALID_USER, VALID_SERVICE_ID, new LoadBalancerCacheRecord(VALID_INSTANCE_ID));
                when(info.getInstanceId()).thenReturn("nowhere");
                assertThat(underTest.shouldFilter(), is(true));
                underTest.run();
                LoadBalancerCacheRecord record = loadBalancerCache.retrieve(VALID_USER, VALID_SERVICE_ID);
                assertThat(record.getInstanceId(), is(VALID_INSTANCE_ID));
            }
        }

        @Nested
        class GivenNoAuthentication {

            @Test
            void dontStoreInstanceInfo() {
                when(info.getInstanceId()).thenReturn(VALID_INSTANCE_ID);
                assertThat(underTest.shouldFilter(), is(true));
                underTest.run();
                assertThat(loadBalancerCache.retrieve(VALID_USER, VALID_SERVICE_ID), is(nullValue()));
            }
        }

        @Nested
        class GivenAuthenticationButNoInstanceInfo {

            @Test
            void whenShouldFilterIsFalse_thenDontStoreInstanceInfo() {
                assertThat(underTest.shouldFilter(), is(false));
                assertThat(loadBalancerCache.retrieve(VALID_USER, VALID_SERVICE_ID), is(nullValue()));
            }
        }
    }
}
