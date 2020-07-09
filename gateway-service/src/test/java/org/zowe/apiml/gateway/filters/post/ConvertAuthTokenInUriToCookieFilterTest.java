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

import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.utils.CleanCurrentRequestContextTest;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class ConvertAuthTokenInUriToCookieFilterTest extends CleanCurrentRequestContextTest {

    private final AuthConfigurationProperties authConfigurationProperties = new AuthConfigurationProperties();
    private final ConvertAuthTokenInUriToCookieFilter filter = new ConvertAuthTokenInUriToCookieFilter(
            authConfigurationProperties);

    @Test
    void doesNotDoAnythingWhenThereIsNoParam() {
        this.filter.run();
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    void doesNotDoAnythingWhenThereIsAnotherParam() {
        Map<String, List<String>> params = new HashMap<>();
        params.put("someParameter", Collections.singletonList("value"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertFalse(ctx.getResponse().getHeaderNames().contains("Set-Cookie"));
    }

    @Test
    void setsCookieForCorrectParameter() {
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/service"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(authConfigurationProperties.getCookieProperties().getCookieName(),
                Collections.singletonList("token"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/service", ctx.getZuulResponseHeaders().get(0).second());
    }

    @Test
    void setsLocationToDashboardForApiCatalog() {
        ctx.setRequest(new MockHttpServletRequest("GET", "/api/v1/apicatalog/"));
        Map<String, List<String>> params = new HashMap<>();
        params.put(authConfigurationProperties.getCookieProperties().getCookieName(),
                Collections.singletonList("token"));
        ctx.setRequestQueryParams(params);
        this.filter.run();
        assertTrue(ctx.getResponse().getHeaders("Set-Cookie").toString().contains("apimlAuthenticationToken=token"));
        assertEquals("Location", ctx.getZuulResponseHeaders().get(0).first());
        assertEquals("http://localhost/api/v1/apicatalog/#/dashboard", ctx.getZuulResponseHeaders().get(0).second());
    }
}
