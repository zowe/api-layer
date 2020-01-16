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

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.netflix.zuul.context.RequestContext;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ConvertAuthTokenInUriToCookieFilterTest {

    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final ConvertAuthTokenInUriToCookieFilter filter = new ConvertAuthTokenInUriToCookieFilter(
            authConfigurationProperties);

    private RequestContext getMockRequestContext() {
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.setResponse(new MockHttpServletResponse());
        return ctx;
    }

    private void runFilter(RequestContext ctx) {
        synchronized (ctx) {
            this.filter.run();
        }
    }

    @Test
    public void doesNotDoAnythingWhenThereIsNoParam() {
        RequestContext ctx = getMockRequestContext();
        runFilter(ctx);
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void doesNotDoAnythingWhenThereIsAnotherParam() {
        RequestContext ctx = getMockRequestContext();
        Map<String, List<String>> params = new HashMap<>();
        params.put("someParameter", Collections.singletonList("value"));
        ctx.setRequestQueryParams(params);
        runFilter(ctx);
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    public void setsCookieForCorrectParameter() {
        RequestContext ctx = getMockRequestContext();
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/service"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(authConfigurationProperties.getCookieProperties().getCookieName(),
                Collections.singletonList("token"));
        ctx.setRequestQueryParams(params);
        runFilter(ctx);
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/service", ctx.getZuulResponseHeaders().get(0).second());
    }

    @Test
    public void setsLocationToDashboardForApiCatalog() {
        RequestContext ctx = getMockRequestContext();
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/apicatalog/"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(authConfigurationProperties.getCookieProperties().getCookieName(),
                Collections.singletonList("token"));
        ctx.setRequestQueryParams(params);
        runFilter(ctx);
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/apicatalog/#/dashboard", ctx.getZuulResponseHeaders().get(0).second());
    }
}
