/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.pre;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class SlashFilterTest {

    private SlashFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new SlashFilter();
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(PROXY_KEY, "/service/ui");
        ctx.set(SERVICE_ID_KEY, "service");
        ctx.setResponse(new MockHttpServletResponse());
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/service/ui");
        ctx.setRequest(mockRequest);
    }

    @Test
    void responseIsModified() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        this.filter.run();
        String location = "";
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    location = header.second();
            }
        }
        assertEquals("/service/ui/", location);
        assertEquals(302, ctx.getResponseStatusCode());
    }

    @Test
    void responseIsModified_OldPathFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        setRequestContextToOldPathFormat(ctx);
        this.filter.run();
        String location = "";
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    location = header.second();
            }
        }
        assertEquals("/ui/service/", location);
        assertEquals(302, ctx.getResponseStatusCode());
    }

    @Test
    void shouldReturnFilterType() {
        String filterType = this.filter.filterType();
        assertEquals("pre", filterType);
    }

    @Test
    void shouldReturnFilterOrder() {
        int filterOrder = this.filter.filterOrder();
        assertEquals(9, filterOrder);
    }

    @Test
    void proxyStartsWithSlash() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "/service/ui");
        this.filter.run();
        String location = "";
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    location = header.second();
            }
        }
        assertEquals("/service/ui/", location);
        assertEquals(302, ctx.getResponseStatusCode());
    }

    @Test
    void proxyStartsWithSlash_OldPathFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        setRequestContextToOldPathFormat(ctx);
        ctx.set(PROXY_KEY, "/ui/service");
        this.filter.run();
        String location = "";
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    location = header.second();
            }
        }
        assertEquals("/ui/service/", location);
        assertEquals(302, ctx.getResponseStatusCode());
    }

    @Test
    void proxyEndsWithSlash() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "service/ui/");
        this.filter.run();
        String location = "";
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    location = header.second();
            }
        }
        assertEquals("/service/ui/", location);
        assertEquals(302, ctx.getResponseStatusCode());
    }

    @Test
    void proxyEndsWithSlash_OldPathFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        setRequestContextToOldPathFormat(ctx);
        ctx.set(PROXY_KEY, "ui/service/");
        this.filter.run();
        String location = "";
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    location = header.second();
            }
        }
        assertEquals("/ui/service/", location);
        assertEquals(302, ctx.getResponseStatusCode());
    }

    @Test
    void proxyIsNull() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, null);
        this.filter.run();
        boolean isLocation = false;
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    isLocation = true;
            }
        }
        assertFalse(isLocation);
        assertEquals(500, ctx.getResponseStatusCode());
    }

    @Test
    void proxyIsEmpty() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "");
        this.filter.run();
        boolean isLocation = false;
        List<Pair<String, String>> zuulResponseHeaders = ctx.getZuulResponseHeaders();
        if (zuulResponseHeaders != null) {
            for (Pair<String, String> header : zuulResponseHeaders) {
                if (header.first().equals("Location"))
                    isLocation = true;
            }
        }
        assertFalse(isLocation);
        assertEquals(500, ctx.getResponseStatusCode());
    }

    @Test
    void shouldFilterUI() {
        assertTrue(this.filter.shouldFilter());
    }

    @Test
    void shouldNotFilterAPI() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/service/api/v1");
        ctx.set(PROXY_KEY, "/service/api/v1");
        ctx.setRequest(mockRequest);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    void shouldNotFilterAPI_OldPathFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        setRequestContextToOldPathFormat(ctx);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/api/v1/service");
        ctx.set(PROXY_KEY, "/api/v1/service");
        ctx.setRequest(mockRequest);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    void shouldNotFilterWhenItEndsWithSlash() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/service/ui/");
        ctx.setRequest(mockRequest);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    void shouldNotFilterWhenItEndsWithSlash_OldPathFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        setRequestContextToOldPathFormat(ctx);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/ui/service/");
        ctx.setRequest(mockRequest);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    void serviceIdIsNull() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, null);
        assertFalse(this.filter.shouldFilter());
    }

    @Test
    void serviceIdIsEmpty() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "");
        assertFalse(this.filter.shouldFilter());
    }

    private void setRequestContextToOldPathFormat(RequestContext ctx) {
        ctx.set(PROXY_KEY, "/ui/service");
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI("/ui/service");
        ctx.setRequest(mockRequest);
    }
}
