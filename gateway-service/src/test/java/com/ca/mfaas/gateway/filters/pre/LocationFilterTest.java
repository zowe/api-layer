/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.filters.pre;

import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

public class LocationFilterTest {

    private LocationFilter filter;

    @Before
    public void setUp() throws Exception {
        this.filter = new LocationFilter();
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(REQUEST_URI_KEY, "/path");
        ctx.set(PROXY_KEY, "api/v1/service");
        ctx.set(SERVICE_ID_KEY, "service");
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(
            new RoutedService("testv1", "ui/r4", "/service/r1"));
        routedServices.addRoutedService(
            new RoutedService("testv4", "api/v2", "/service/v2"));
        routedServices.addRoutedService(
            new RoutedService("testv2", "test", "/service/test1"));
        routedServices.addRoutedService(
            new RoutedService("testv3", "api/v1", "/service/v1"));
        this.filter.addRoutedServices("service", routedServices);
    }

    @Test
    public void urlNotDefinedInMetadataIsNotModified() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "service1");
        ctx.set(PROXY_KEY, "service1");
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void urlDefinedInMetadataIsModified() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        this.filter.run();
        assertEquals("/service/v1/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void requestURIStartsWithoutSlash() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "path");
        this.filter.run();
        assertEquals("/service/v1/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void proxyStartsWithSlash() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "/api/v2/service");
        this.filter.run();
        assertEquals("/service/v2/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void proxyEndsWithSlash() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "api/v2/service/");
        this.filter.run();
        assertEquals("/service/v2/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void serviceIdIsEmpty() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "");
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void serviceIdIsNull() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, null);
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void proxyIsEmpty() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "");
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void proxyIsNull() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, null);
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void requestPathIsEmpty() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "");
        this.filter.run();
        assertEquals("/service/v1", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void requestPathIsNull() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, null);
        this.filter.run();
        assertEquals(null, ctx.get(REQUEST_URI_KEY));
    }

    @Test
    public void shouldReturnFilterType() {
        String filterType = this.filter.filterType();
        assertEquals("pre", filterType);
    }

    @Test
    public void shouldFilterShouldReturnTrue() {
        Boolean filterFlag = this.filter.shouldFilter();
        assertEquals(true, filterFlag);
    }

    @Test
    public void shouldReturnFilterOrder() {
        int filterOrder = this.filter.filterOrder();
        assertEquals(6, filterOrder);
    }

    @Test
    public void normalizeOriginalPathShouldReturnEmptyString() {
        LocationFilter filter = new LocationFilter();
        final RequestContext ctx = RequestContext.getCurrentContext();
        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(
            new RoutedService("testv1", "api/v1", null));
        filter.addRoutedServices("service", routedServices);
        filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }
}
