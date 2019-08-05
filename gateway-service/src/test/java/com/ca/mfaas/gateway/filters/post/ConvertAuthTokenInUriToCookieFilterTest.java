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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.*;

import com.ca.apiml.security.config.AuthConfigurationProperties;
import com.netflix.zuul.context.RequestContext;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class ConvertAuthTokenInUriToCookieFilterTest {

    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final ConvertAuthTokenInUriToCookieFilter filter = new ConvertAuthTokenInUriToCookieFilter(authConfigurationProperties);

    @Before
    public void setUp() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.setResponse(new MockHttpServletResponse());
    }

    @Test
    public void doesNotDoAnythingWhenThereIsNoParam() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        this.filter.run();
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void doesNotDoAnythingWhenThereIsAnotherParam() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        Map<String, List<String>> params = new HashMap<>();
        params.put("someParameter", Collections.singletonList("value"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void setsCookieForCorrectParameter() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/service"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(authConfigurationProperties.getCookieProperties().getCookieName(), Collections.singletonList("token"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/service", ctx.getZuulResponseHeaders().get(0).second());
    }

    @Test
    public void setsLocationToDashboardForApiCatalog() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/apicatalog/"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(authConfigurationProperties.getCookieProperties().getCookieName(), Collections.singletonList("token"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/apicatalog/#/dashboard", ctx.getZuulResponseHeaders().get(0).second());
    }
}
