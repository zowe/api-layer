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
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

public class ServiceNotFoundFilter extends ZuulFilter {
    RequestContextProvider contextProvider;

    public ServiceNotFoundFilter() {
        this.contextProvider = new RequestContextProviderThreadLocal();
    }

    public ServiceNotFoundFilter(RequestContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 1;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = contextProvider.context();
        String serviceId = (String) currentContext.get(SERVICE_ID_KEY);
        return Strings.isEmpty(serviceId);
    }

    @Override
    public Object run() throws ZuulException {
        throw new ZuulException("", HttpStatus.NOT_FOUND.value(), "");
    }
}
