/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.filters.post;

import com.ca.mfaas.security.config.SecurityConfigurationProperties;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConvertAuthTokenInUriToCookieFilterTest {

    private ConvertAuthTokenInUriToCookieFilter filter;

    @Before
    public void setUp() throws Exception {
        SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
        this.filter = new ConvertAuthTokenInUriToCookieFilter(securityConfigurationProperties);
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.setResponse(new MockHttpServletResponse());
    }

    @Test
    public void doesNotDoAnythingWhenThereIsNoParam() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        this.filter.run();
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void doesNotDoAnythingWhenThereIsAnotherParam() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        Map<String, List<String>> params = new HashMap<>();
        params.put("someParameter", Arrays.asList("value"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void setsCookieForCorrectParameter() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/service"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(ConvertAuthTokenInUriToCookieFilter.TOKEN_KEY, Arrays.asList("token"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/service", ctx.getZuulResponseHeaders().get(0).second());
    }

    @Test
    public void setsLocationToDashboardForApiCatalog() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/apicatalog/"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(ConvertAuthTokenInUriToCookieFilter.TOKEN_KEY, Arrays.asList("token"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/apicatalog/#/dashboard", ctx.getZuulResponseHeaders().get(0).second());
    }
}
