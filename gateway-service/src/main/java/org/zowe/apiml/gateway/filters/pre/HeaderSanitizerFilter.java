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

import com.netflix.zuul.context.RequestContext;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;

/**
 * Filter is used to clear values in request headers. Provide list of such headers in property source and they will
 * be set to null. Proper header values can then be set in next filter.
 */
@Component
@NoArgsConstructor
@AllArgsConstructor
public class HeaderSanitizerFilter extends PreZuulFilter {
    @Value(value = "${apiml.security.headersToBeCleared:}")
    private String[] headersToBeCleared;

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 5;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        Map<String, String> requestHeaders = new HashMap<>();

        includeHeadersFromZuul(context, requestHeaders);
        includeHeadersFromRequest(context, requestHeaders);

        nullHeadersOfInterestIntoZuulRequestHeaders(context, requestHeaders);

        return null;
    }

    private void nullHeadersOfInterestIntoZuulRequestHeaders(RequestContext context, Map<String, String> requestHeaders) {
        Arrays.stream(headersToBeCleared).forEach( toBeCleared -> //for each header to be cleared
            requestHeaders.entrySet().stream() //in all requestHeaders
                .filter(entry -> entry.getKey().equalsIgnoreCase(toBeCleared)) // find headers that match ignoring case
                .forEach(entry -> context.addZuulRequestHeader(entry.getKey(), null)) // and null it
        );
    }

    private void includeHeadersFromZuul(RequestContext context, Map<String, String> requestHeaders) {
        requestHeaders.putAll(context.getZuulRequestHeaders());
    }

    private void includeHeadersFromRequest(RequestContext context, Map<String, String> requestHeaders) {
        Enumeration<String> headerNames = context.getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            requestHeaders.put(headerName, "");
        }
    }

}
