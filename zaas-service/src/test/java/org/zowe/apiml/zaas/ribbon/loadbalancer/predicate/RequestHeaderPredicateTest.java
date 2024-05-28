/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.ribbon.loadbalancer.predicate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.zowe.apiml.zaas.ribbon.loadbalancer.LoadBalancingContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestHeaderPredicateTest {

    private RequestContext rctx;

    @BeforeEach
    void setup() {
        rctx = RequestContext.getCurrentContext();
        rctx.clear();
    }

    @Nested
    class WithoutHeader {

        @Test
        void doesNotFilter() {
            rctx.setRequest(new MockHttpServletRequest());
            InstanceInfo info = mock(InstanceInfo.class);
            DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
            LoadBalancingContext lbctx = new LoadBalancingContext("key", info);
            RequestHeaderPredicate predicate = new RequestHeaderPredicate();
            assertTrue(predicate.apply(lbctx, server));
        }
    }


    @Nested
    class WhitHeader {

        MockHttpServletRequest httpServletRequest;
        InstanceInfo info;
        DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
        LoadBalancingContext lbctx;

        @BeforeEach
        void setUp() {
            httpServletRequest = new MockHttpServletRequest();
            rctx.setRequest(httpServletRequest);
            info = mock(InstanceInfo.class);
            when(server.getInstanceInfo()).thenReturn(info);
            lbctx = new LoadBalancingContext("key", info);
        }

        @ParameterizedTest(name = "{index} - testedHeader: {0} ")
        @CsvSource(value = {"X-InstanceId", "x-instanceid", "X-INSTANCEID"})
        void filtersOnInstanceId(String headerName) {
            httpServletRequest.addHeader(headerName, "server1");
            RequestHeaderPredicate predicate = new RequestHeaderPredicate();
            when(info.getInstanceId()).thenReturn("server1");
            assertTrue(predicate.apply(lbctx, server));
            when(info.getInstanceId()).thenReturn("server2");
            assertFalse(predicate.apply(lbctx, server));
        }

    }

    @Nested
    class NotZuulRequest {

        @Test
        void alwaysValid() {
            RequestHeaderPredicate predicate = new RequestHeaderPredicate();
            assertTrue(predicate.apply(new LoadBalancingContext("key", null), null));

        }
    }

}
