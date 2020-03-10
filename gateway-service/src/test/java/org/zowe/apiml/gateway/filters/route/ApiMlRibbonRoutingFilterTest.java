/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.route;

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class ApiMlRibbonRoutingFilterTest {
    private ApiMlRibbonRoutingFilter underTest;
    private MockHttpServletRequest mockHttpServletRequest;
    private RequestContext context;

    @BeforeEach
    void setUp() {
        mockHttpServletRequest = new MockHttpServletRequest(HttpMethod.GET.name(), "https://localhost:10010/api/v2/user-service/pets/1");

        context = new RequestContext();
        context.setRequest(mockHttpServletRequest);
        context.set(SERVICE_ID_KEY, "user-service");
        RequestContext.testSetCurrentContext(context);

        underTest = new ApiMlRibbonRoutingFilter(
            new ProxyRequestHelper(new ZuulProperties()),
            mock(RibbonCommandFactory.class),
            new ArrayList<>()
        );
    }

    @Test
    public void givenRefererAndOriginIsNotPresent_whenRequestPassesThrough_thenTheHeadersAreAdded() {
        RibbonCommandContext result = underTest.buildCommandContext(context);

        String expectedCaller = "localhost:80";
        assertThat(result.getHeaders().getFirst("Origin"), is(expectedCaller));
        assertThat(result.getHeaders().getFirst("Referer"), is(expectedCaller));
    }

    @Test
    public void givenRefererAndOriginIsPresent_whenRequestPassesThrough_thenTheHeadersArePasseThrough() {
        String theOriginalCaller = "https://test.net:8080";
        mockHttpServletRequest.addHeader("Origin", theOriginalCaller);
        mockHttpServletRequest.addHeader("Referer", theOriginalCaller);

        RibbonCommandContext result = underTest.buildCommandContext(context);

        MultiValueMap<String, String> headers = result.getHeaders();
        assertThat(headers.getFirst("Origin"), is(theOriginalCaller));
        assertThat(headers.getFirst("Referer"), is(theOriginalCaller));
    }
}
