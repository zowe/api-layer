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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;
import org.zowe.apiml.gateway.ribbon.loadbalancer.model.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.security.service.HttpAuthenticationService;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostStoreLoadBalancerCacheFilterTest {

    PostStoreLoadBalancerCacheFilter postStoreLoadBalancerCacheFilter;

    private RequestContext ctx;
    private InstanceInfo info;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private Authentication authentication;
    HttpAuthenticationService authenticationService;
    String VALID_USER = "annie";
    LoadBalancerCache loadBalancerCache;

    @BeforeEach
    void setUp() {
        authenticationService = mock(HttpAuthenticationService.class);
        ctx = RequestContext.getCurrentContext();
        ctx.clear();
        info = mock(InstanceInfo.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        ctx.setRequest(request);
        ctx.setResponse(response);
        ctx.set(SERVICE_ID_KEY, "instance");
        authentication = mock(Authentication.class);
        loadBalancerCache = new LoadBalancerCache();

        postStoreLoadBalancerCacheFilter = new PostStoreLoadBalancerCacheFilter(authenticationService, loadBalancerCache);
    }

    @Test
    void verifyFilterProperties() {
        assertThat(postStoreLoadBalancerCacheFilter.shouldFilter(), is(true));
        assertThat(postStoreLoadBalancerCacheFilter.filterOrder(), is(SEND_RESPONSE_FILTER_ORDER - 1));
        assertThat(postStoreLoadBalancerCacheFilter.filterType(), is(POST_TYPE));
    }

    @Nested
    class GivenAuthenticationAndInstanceInfo {

        @Test
        void addInstanceInfoToCache() {

            when(info.getInstanceId()).thenReturn("instance");
            RequestContextUtils.setInstanceInfo(info);

            when(authenticationService.getAuthenticatedUser(any())).thenReturn(Optional.of(VALID_USER));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            postStoreLoadBalancerCacheFilter.run();
            Mockito.verify(info, times(1)).getInstanceId();
            assertThat(postStoreLoadBalancerCacheFilter.getLoadBalancerCache().getCache().toString(), containsString("{annie:instance=LoadBalancerCacheRecord(instanceId=instance, creationTime="));
            assertThat(postStoreLoadBalancerCacheFilter.getLoadBalancerCache().getCache().size(), is(1));
        }

        @Test
        void doNotCacheInstanceInfo() {
            loadBalancerCache = mock(LoadBalancerCache.class);
            when(loadBalancerCache.retrieve(anyString(), anyString())).thenReturn(new LoadBalancerCacheRecord("instance"));

            postStoreLoadBalancerCacheFilter = new PostStoreLoadBalancerCacheFilter(authenticationService, loadBalancerCache);
            when(info.getInstanceId()).thenReturn("instance");
            RequestContextUtils.setInstanceInfo(info);

            when(authenticationService.getAuthenticatedUser(any())).thenReturn(Optional.of(VALID_USER));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            postStoreLoadBalancerCacheFilter.run();
            Mockito.verify(info, times(0)).getInstanceId();
            assertThat(postStoreLoadBalancerCacheFilter.getLoadBalancerCache().getCache().size(), is(0));
        }

    }

    @Nested
    class GivenNoAuthentication {

        @Test
        void filterShouldReturnNull() {

            when(info.getInstanceId()).thenReturn("instance");
            RequestContextUtils.setInstanceInfo(info);

            when(authenticationService.getAuthenticatedUser(any())).thenReturn(Optional.empty());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            postStoreLoadBalancerCacheFilter.run();
            assertThat(postStoreLoadBalancerCacheFilter.getLoadBalancerCache().getCache().size(), is(0));
        }
    }

    @Nested
    class GivenAuthenticationButNoInstanceInfo {

        @Test
        void filterShouldReturnNull() {

            when(authenticationService.getAuthenticatedUser(any())).thenReturn(Optional.of(VALID_USER));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            postStoreLoadBalancerCacheFilter.run();
            Mockito.verify(info, times(0)).getInstanceId();
            assertThat(postStoreLoadBalancerCacheFilter.getLoadBalancerCache().getCache().size(), is(0));
        }
    }
}
