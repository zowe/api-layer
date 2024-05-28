/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.filters.post;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.util.Pair;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.zaas.ribbon.RequestContextUtils;
import org.zowe.apiml.zaas.ribbon.loadbalancer.LoadBalancerConstants;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

class RoutedInstanceIdFilterTest {

    @Test
    void verifyFilterProperties() {
        ZuulFilter underTest = new RoutedInstanceIdFilter();
        assertThat(underTest.shouldFilter(), is(true));
        assertThat(underTest.filterOrder(), is(SEND_RESPONSE_FILTER_ORDER - 1));
        assertThat(underTest.filterType(), is(POST_TYPE));
    }

    @Nested
    class givenInstanceIdInRequest {

        @Test
        void writesHeaderWithInstanceId() throws Exception {
            RequestContext ctx = RequestContext.getCurrentContext();
            ctx.clear();
            InstanceInfo info = mock(InstanceInfo.class);
            when(info.getInstanceId()).thenReturn("suziquatro");
            ctx.set(RequestContextUtils.INSTANCE_INFO_KEY, info);
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            ctx.setRequest(request);
            ctx.setResponse(response);
            ZuulFilter underTest = new RoutedInstanceIdFilter();
            underTest.run();
            assertThat(ctx.getZuulResponseHeaders().stream().map(Pair::first).collect(Collectors.toList())
                , containsInAnyOrder(LoadBalancerConstants.INSTANCE_HEADER_KEY));
            assertThat(ctx.getZuulResponseHeaders().stream().map(Pair::second).collect(Collectors.toList())
                , containsInAnyOrder("suziquatro"));
        }
    }
}
