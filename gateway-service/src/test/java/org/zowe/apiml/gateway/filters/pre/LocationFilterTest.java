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

import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_URI_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

class LocationFilterTest {

    private LocationFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new LocationFilter();
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.set(REQUEST_URI_KEY, "/path");
        ctx.set(PROXY_KEY, "service/api/v1");
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
    void urlNotDefinedInMetadataIsNotModified() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "service1");
        ctx.set(PROXY_KEY, "service1");
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void urlDefinedInMetadataIsModified() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        this.filter.run();
        assertEquals("/service/v1/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void urlDefinedInMetadataIsModified_OldProxyFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "api/v1/service");
        this.filter.run();
        assertEquals("/service/v1/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void requestURIStartsWithoutSlash() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "path");
        this.filter.run();
        assertEquals("/service/v1/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void proxyStartsWithSlash() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "/service/api/v2");
        this.filter.run();
        assertEquals("/service/v2/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void proxyStartsWithSlash_OldProxyFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "/api/v2/service");
        this.filter.run();
        assertEquals("/service/v2/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void proxyEndsWithSlash() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "service/api/v2/");
        this.filter.run();
        assertEquals("/service/v2/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void proxyEndsWithSlash_OldProxyFormat() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "api/v2/service/");
        this.filter.run();
        assertEquals("/service/v2/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void serviceIdIsEmpty() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "");
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void serviceIdIsNull() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, null);
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void proxyIsEmpty() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, "");
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void proxyIsNull() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(PROXY_KEY, null);
        this.filter.run();
        assertEquals("/path", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void requestPathIsEmpty() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, "");
        this.filter.run();
        assertEquals("/service/v1/", ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void requestPathIsNull() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(REQUEST_URI_KEY, null);
        this.filter.run();
        assertNull(ctx.get(REQUEST_URI_KEY));
    }

    @Test
    void shouldReturnFilterType() {
        String filterType = this.filter.filterType();
        assertEquals("pre", filterType);
    }

    @Test
    void shouldFilterShouldReturnTrue() {
        Boolean filterFlag = this.filter.shouldFilter();
        assertEquals(true, filterFlag);
    }

    @Test
    void shouldReturnFilterOrder() {
        int filterOrder = this.filter.filterOrder();
        assertEquals(8, filterOrder);
    }

    @Test
    void normalizeOriginalPathShouldReturnEmptyString() {
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
