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

import java.util.List;
import java.util.Map;
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
        Optional<ServiceInstance> validInstance = getInstanceInfoForUri(context.getRequest().getRequestURI());
        if (validInstance.isPresent()) {
            ServiceInstance serviceInstance = validInstance.get();
            String headersToSanitize = serviceInstance.getMetadata().get("apiml.headersToIgnore");
            if (headersToSanitize != null && !headersToSanitize.trim().isEmpty()) {
                String[] headers = StringUtils.stripAll(headersToSanitize.split(","));
                proxyRequestHelper.addIgnoredHeaders(headers);
            }
        }

        return null;
    }

    // TODO DRY - move to utils or something
    Optional<ServiceInstance> getInstanceInfoForUri(String requestUri) {
        // Compress only if there is valid instance with relevant metadata.
        String[] uriParts = requestUri.split("/");
        List<ServiceInstance> instances;
        if (uriParts.length < 2) {
            return Optional.empty();
        }
        if ("api".equals(uriParts[1]) || "ui".equals(uriParts[1])) {
            if (uriParts.length < 4) {
                return Optional.empty();
            }
            instances = discoveryClient.getInstances(uriParts[3]);
        } else {
            instances = discoveryClient.getInstances(uriParts[1]);
        }
        if (instances == null || instances.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(instances.get(0));
    }
}
