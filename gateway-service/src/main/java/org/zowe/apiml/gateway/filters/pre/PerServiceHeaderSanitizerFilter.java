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
import com.netflix.zuul.exception.ZuulException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.routing.RouteUtil;

import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

@Component
@RequiredArgsConstructor
public class PerServiceHeaderSanitizerFilter extends ZuulFilter {

    private final DiscoveryClient discoveryClient;
    private final ProxyRequestHelper proxyRequestHelper;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 7;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        Optional<ServiceInstance> validInstance = RouteUtil.getInstanceInfoForUri(context.getRequest().getRequestURI(), discoveryClient);

        if (validInstance.isPresent()) {
            ServiceInstance serviceInstance = validInstance.get();
            String headersToIgnore = serviceInstance.getMetadata().get("apiml.headersToIgnore");

            if (headersToIgnore != null && !headersToIgnore.trim().isEmpty()) {
                String[] headers = StringUtils.stripAll(headersToIgnore.split(","));
                proxyRequestHelper.addIgnoredHeaders(headers);
            }
        }

        return null;
    }
}
