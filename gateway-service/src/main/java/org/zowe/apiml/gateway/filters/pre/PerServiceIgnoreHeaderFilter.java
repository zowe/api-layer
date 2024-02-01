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

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.services.ServiceInstancesUtils;

import java.util.List;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;

@Component
@RequiredArgsConstructor
public class PerServiceIgnoreHeaderFilter extends PreZuulFilter {

    private final DiscoveryClient discoveryClient;
    private final ProxyRequestHelper proxyRequestHelper;

    @Autowired
    public PerServiceIgnoreHeaderFilter(DiscoveryClient discoveryClient, ZuulProperties zuulProperties) {
        this.discoveryClient = discoveryClient;
        this.proxyRequestHelper = new ProxyRequestHelper(zuulProperties);
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
    public Object run() {
        List<ServiceInstance> serviceInstances = ServiceInstancesUtils.getServiceInstancesFromDiscoveryClient(discoveryClient);
        if (serviceInstances != null && !serviceInstances.isEmpty()) {
            ServiceInstance serviceInstance = serviceInstances.get(0);
            String headersToIgnore = serviceInstance.getMetadata().get("apiml.headersToIgnore");

            if (headersToIgnore != null && !headersToIgnore.trim().isEmpty()) {
                String[] headers = StringUtils.stripAll(headersToIgnore.split(","));
                proxyRequestHelper.addIgnoredHeaders(headers);
            }
        }
        return null;
    }
}
