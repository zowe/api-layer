/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.filters.pre;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

class ServiceNotFoundFilterTest {
    private ZuulFilter underTest;
    private RequestContext context;

    @BeforeEach
    public void prepareFilterUnderTest() {
        context = RequestContext.getCurrentContext();
        context.clear();

        underTest = new ServiceNotFoundFilter();
        MonitoringHelper.initMocks();
    }

    @Test
    void givenThereIsNoServiceId_whenTheUserRequestsThePath_then404IsProperlyReturned() {
        Boolean ignoreThisFilter = underTest.shouldFilter();
        assertThat(ignoreThisFilter, is(true));

        // Exception represents 404
        assertThrows(ZuulException.class, () -> {
            underTest.run();
        });
    }

    @Test
    void givenThereIsValidServiceId_whenTheUserRequestsThePath_thenThisFilterIsIgnored() {
        context.set(SERVICE_ID_KEY, "validServiceId");

        Boolean ignoreThisFilter = underTest.shouldFilter();
        assertThat(ignoreThisFilter, is(false));
    }

    @Test
    void givenValidSetup_whenTheFilterIsCreated_thenTheCorrectConfigurationParametersAreProvided() {
        assertThat(underTest.filterOrder(), is(PRE_DECORATION_FILTER_ORDER + 1));
        assertThat(underTest.filterType(), is(PRE_TYPE));
    }
}
