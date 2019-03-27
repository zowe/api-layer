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

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 * Uses the authentication token provided as a query parameter and puts it to
 * the expected place (cookie).
 */
public class ConvertAuthTokenInUriToCookieFilter extends ZuulFilter {
    static final String TOKEN_KEY = "apimlAuthenticationToken";

    private final SecurityConfigurationProperties securityConfigurationProperties;

    public ConvertAuthTokenInUriToCookieFilter(SecurityConfigurationProperties securityConfigurationProperties) {
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    public String filterType() {
        return POST_TYPE;
    }

    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 1;
    }

    public boolean shouldFilter() {
        return true;
    }

    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        if ((context.getRequestQueryParams() != null) && context.getRequestQueryParams().containsKey(TOKEN_KEY)) {
            HttpServletResponse servletResponse = context.getResponse();
            SecurityConfigurationProperties.CookieProperties cp = securityConfigurationProperties.getCookieProperties();
            Cookie cookie = new Cookie(cp.getCookieName(), context.getRequestQueryParams().get(TOKEN_KEY).get(0));
            cookie.setPath(cp.getCookiePath());
            cookie.setSecure(true);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(cp.getCookieMaxAge());
            cookie.setComment(cp.getCookieComment());
            servletResponse.addCookie(cookie);

            String url = context.getRequest().getRequestURL().toString();
            String newUrl;
            if (url.endsWith("/apicatalog/")) {
                newUrl = url + "#/dashboard";
            } else {
                newUrl = url;
            }
            context.addZuulResponseHeader("Location", newUrl);
            context.setResponseStatusCode(HttpServletResponse.SC_MOVED_TEMPORARILY);
        }
        return null;
    }
}
