/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.route;

import com.netflix.zuul.context.RequestContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.ribbon.support.RibbonRequestCustomizer;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RIBBON_ROUTING_FILTER_ORDER;

/**
 * If the context for the command misses the Origin or Referer the values are provided by the API ML.
 * It reflects GitHub issue #285
 */
public class ApiMlRibbonRoutingFilter extends RibbonRoutingFilter {
    public ApiMlRibbonRoutingFilter(ProxyRequestHelper helper, RibbonCommandFactory<?> ribbonCommandFactory, List<RibbonRequestCustomizer> requestCustomizers) {
        super(helper, ribbonCommandFactory, requestCustomizers);
    }

    @Override
    public int filterOrder() {
        return RIBBON_ROUTING_FILTER_ORDER - 1;
    }

    @Override
    protected RibbonCommandContext buildCommandContext(RequestContext context) {
        RibbonCommandContext initialCommandContext = super.buildCommandContext(context);

        addOriginAndRefererIfNotPresent(context, initialCommandContext.getHeaders());

        return initialCommandContext;
    }

    /**
     * Verify if the headers are present. If the headers aren't present provide them based on the remote host, address
     * and port information.
     * @param context Current Zuul Context
     * @param headers Headers to be modified
     */
    private void addOriginAndRefererIfNotPresent(RequestContext context, MultiValueMap<String, String> headers) {
        HttpServletRequest request = context.getRequest();

        String theAddressTheRequestComesFrom = request.getRemoteHost() != null ? request.getRemoteHost() : request.getRemoteAddr();
        theAddressTheRequestComesFrom += ":" + request.getRemotePort();

        if (headers.get("Origin") == null) {
            headers.set("Origin", theAddressTheRequestComesFrom);
        }
        if (headers.get("Referer") == null) {
            headers.add("Referer", theAddressTheRequestComesFrom);
        }
    }
}
