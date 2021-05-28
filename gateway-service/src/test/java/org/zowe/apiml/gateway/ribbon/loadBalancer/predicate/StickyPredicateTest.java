/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.ribbon.loadBalancer.predicate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.gateway.ribbon.loadBalancer.LoadBalancingContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StickyPredicateTest {

    private RequestContext rctx;

    @BeforeEach
    void setup() {
        rctx = RequestContext.getCurrentContext();
        rctx.clear();
    }

    @Test
    void noHeader() {
        rctx.setRequest(new MockHttpServletRequest());
        InstanceInfo info = mock(InstanceInfo.class);
        DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
        LoadBalancingContext lbctx = new LoadBalancingContext("key", info);
        StickyPredicate predicate = new StickyPredicate();
        assertTrue(predicate.apply(lbctx, server));
    }

    @Nested
    class WhitHeader {
        @Test
        void serverMatchesHeader() {
            MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
            httpServletRequest.addHeader("X-host", "server1");
            rctx.setRequest(httpServletRequest);
            InstanceInfo info = mock(InstanceInfo.class);
            DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
            when(server.getInstanceInfo()).thenReturn(info);
            when(info.getInstanceId()).thenReturn("server1");
            LoadBalancingContext lbctx = new LoadBalancingContext("key", info);
            StickyPredicate predicate = new StickyPredicate();
            assertTrue(predicate.apply(lbctx, server));
        }

        @Test
        void serverDoesntMatchHeader() {
            MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
            httpServletRequest.addHeader("X-host", "server1");
            rctx.setRequest(httpServletRequest);
            InstanceInfo info = mock(InstanceInfo.class);
            DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
            when(server.getInstanceInfo()).thenReturn(info);
            when(info.getInstanceId()).thenReturn("server2");
            LoadBalancingContext lbctx = new LoadBalancingContext("key", info);
            StickyPredicate predicate = new StickyPredicate();
            assertFalse(predicate.apply(lbctx, server));
        }
    }

}
