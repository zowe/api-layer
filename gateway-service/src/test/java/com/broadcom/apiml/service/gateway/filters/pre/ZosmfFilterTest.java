/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.gateway.filters.pre;

import com.broadcom.apiml.service.security.token.TokenService;
import com.netflix.zuul.context.RequestContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@Ignore
public class ZosmfFilterTest {

    private static final String TOKEN = "token";
    private static final String LTPA_TOKEN = "ltpaToken";
    private static final String MY_COOKIE = "myCookie=MYCOOKIE";

    private ZosmfFilter filter;
    private TokenService tokenService;

    @Before
    public void setUp() throws Exception {
        this.tokenService = mock(TokenService.class);
        this.filter = new ZosmfFilter(tokenService);
        RequestContext ctx = RequestContext.getCurrentContext();
        ctx.clear();
        ctx.setResponse(new MockHttpServletResponse());
    }

    @Test
    public void shouldFilterZosmfRequests() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "zosmftest");

        assertTrue(this.filter.shouldFilter());
    }

    @Test
    public void shouldNotFilterOtherServiceRequests() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "testservice");

        assertFalse(this.filter.shouldFilter());
    }

    @Test
    public void shouldAddLtpaTokenToZosmfRequests() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "zosmftest");
        when(tokenService.getToken(ctx.getRequest())).thenReturn(TOKEN);
        when(tokenService.getLtpaToken(TOKEN)).thenReturn(LTPA_TOKEN);

        this.filter.run();

        assertTrue(ctx.getZuulRequestHeaders().get("cookie").contains(LTPA_TOKEN));
    }

    @Test
    public void shouldPassWhenLtpaTokenIsMissing() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "zosmftest");
        when(tokenService.getToken(ctx.getRequest())).thenReturn(TOKEN);
        when(tokenService.getLtpaToken(TOKEN)).thenReturn(null);

        this.filter.run();

        assertEquals(null, ctx.getZuulRequestHeaders().get("cookie"));
    }

    @Test
    public void shouldPassWhenJwtTokenIsMissing() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "zosmftest");
        when(tokenService.getToken(ctx.getRequest())).thenReturn(null);
        when(tokenService.getLtpaToken(null)).thenReturn(null);

        this.filter.run();

        assertEquals(null, ctx.getZuulRequestHeaders().get("cookie"));
    }

    @Test
    public void shouldKeepExistingCookies() throws Exception {
        final RequestContext ctx = RequestContext.getCurrentContext();
        ctx.set(SERVICE_ID_KEY, "zosmftest");
        ctx.addZuulRequestHeader("Cookie", MY_COOKIE);

        when(tokenService.getToken(ctx.getRequest())).thenReturn(TOKEN);
        when(tokenService.getLtpaToken(TOKEN)).thenReturn(LTPA_TOKEN);

        this.filter.run();

        assertTrue(ctx.getZuulRequestHeaders().get("cookie").contains(LTPA_TOKEN));
        assertTrue(ctx.getZuulRequestHeaders().get("cookie").contains(MY_COOKIE));
    }
}
