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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class CustomSendErrorFilterTest {
    private static final String SERVICE_ID = "serviceId";

    private CustomSendErrorFilter filter = null;
    private RequestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(SERVICE_ID_KEY, SERVICE_ID);
        ctx.setThrowable(new Exception("test"));
        ctx.set(CustomSendErrorFilter.ERROR_FILTER_RAN, false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ctx.setRequest(request);
        ctx.setResponse(response);

        this.filter = new CustomSendErrorFilter();
    }

    @Test
    void givenContextToRun_whenTestIfFilterRuns_shouldRunErrorFilter() {
        assertTrue(filter.shouldFilter());
    }

    @Test
    void givenContextToNotRun_whenTestIfFilterRuns_shouldRunErrorFilter() {
        ctx.set(CustomSendErrorFilter.ERROR_FILTER_RAN, true);
        assertFalse(filter.shouldFilter());
    }

    @Test
    void givenFilter_whenGetOrder_ShouldBeOne() {
        int filterOrder = filter.filterOrder();
        assertEquals(1, filterOrder);
    }
}
