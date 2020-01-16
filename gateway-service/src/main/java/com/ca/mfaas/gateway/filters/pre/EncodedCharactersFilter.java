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

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 *  This filter should run on all requests for services, which do not have enabled encoded characters in URL
 *
 *  Special characters encoding is enabled on Tomcat and Spring Firewall so this filter takes over responsibility
 *  for filtering them.
 *  Encoded characters in URL are allowed only for services that have it explicitly configured on the metadata.
 *
 */

@RequiredArgsConstructor
public class EncodedCharactersFilter extends ZuulFilter {

    private final DiscoveryClient discoveryClient;
    public static final String METADATA_KEY = "apiml.enableUrlEncodedCharacters";

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        boolean shouldFilter = true;

        RequestContext context = RequestContext.getCurrentContext();
        final String serviceId = (String) context.get(SERVICE_ID_KEY);

        List<ServiceInstance> instanceList = discoveryClient.getInstances(serviceId);

        List<Map<String, String>> enabledList = instanceList.stream()
            .map(ServiceInstance::getMetadata)
            .filter( metadata -> String.valueOf(true).equalsIgnoreCase(metadata.get(METADATA_KEY)) )
            .collect(Collectors.toList());

        if( enabledList.size() == instanceList.size() ) {
            shouldFilter = false;
        }

        return shouldFilter;
    }

    @Override
    public Object run() {

        // TODO Chris part

        return null;
    }
}
