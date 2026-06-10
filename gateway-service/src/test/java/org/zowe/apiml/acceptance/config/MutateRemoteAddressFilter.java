/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.config;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

public class MutateRemoteAddressFilter extends ZuulFilter {

    public final AtomicReference<String> proxyAddressReference = new AtomicReference<>();
    private final String originalProxyAddressProperty;

    MutateRemoteAddressFilter(String proxyAddress) {
        originalProxyAddressProperty = proxyAddress;
        proxyAddressReference.set(originalProxyAddressProperty);
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();

        ctx.setRequest(
            new HttpServletRequestWrapper(ctx.getRequest()) {
                @Override
                public String getRemoteAddr() {
                    return proxyAddressReference.get();
                }
            }
        );
        return null;
    }

    public void reset() {
        proxyAddressReference.set(originalProxyAddressProperty);
    }
}
