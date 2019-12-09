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

import com.ca.mfaas.util.UrlUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PROXY_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Must be run after PreDecorationFilter. This will set Proxy, ServiceId and other variables in RequestContext
 */
public class SlashFilter extends ZuulFilter {

    private static final String UI_IDENTIFIER = "ui/";

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        String url = context.getRequest().getRequestURL().toString().toLowerCase();
        String serviceId = (String) context.get(SERVICE_ID_KEY);
        String proxy = (String) context.get(PROXY_KEY);
        boolean checkProxy = (proxy != null) && proxy.toLowerCase().contains(UI_IDENTIFIER);
        boolean checkServiceId = (serviceId != null) && !serviceId.isEmpty() && url.endsWith(serviceId);
        return checkProxy && checkServiceId;
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 2;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();
        String proxy = UrlUtils.removeFirstAndLastSlash((String) context.get(PROXY_KEY));
        if (proxy != null && !proxy.isEmpty()) {
            context.setSendZuulResponse(false);
            context.addZuulResponseHeader("Location", "/" + proxy + "/");
            context.setResponseStatusCode(HttpServletResponse.SC_MOVED_TEMPORARILY);
        }
        return null;
    }

}

