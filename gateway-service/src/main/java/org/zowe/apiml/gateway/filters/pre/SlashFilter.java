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

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.zowe.apiml.util.UrlUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * Must be run after PreDecorationFilter. This will set Proxy, ServiceId and other variables in RequestContext
 */
public class SlashFilter extends ZuulFilter {

    private static final Pattern REGEX_CONTAINS_UI_PATH = Pattern.compile("(ui/)|(/ui)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGEX_END_WITH_UI_ROUTE = Pattern.compile(".*/ui(/v[0-9])?$", // Optional version after ui
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();
        String url = context.getRequest().getRequestURL().toString().toLowerCase();
        String serviceId = (String) context.get(SERVICE_ID_KEY);
        String proxy = (String) context.get(PROXY_KEY);

        boolean checkProxy = (proxy != null) && REGEX_CONTAINS_UI_PATH.matcher(proxy).find();
        boolean checkServiceId = (serviceId != null) && !serviceId.isEmpty() && url.contains(serviceId);

        boolean oldPathFormatShouldFilter = checkServiceId && url.endsWith(serviceId);
        boolean newPathFormatShouldFilter = checkProxy && REGEX_END_WITH_UI_ROUTE.matcher(url).find();

        return checkProxy && checkServiceId && (oldPathFormatShouldFilter || newPathFormatShouldFilter);
    }

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 4;
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

