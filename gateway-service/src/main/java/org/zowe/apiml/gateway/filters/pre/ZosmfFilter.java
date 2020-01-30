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

import org.zowe.apiml.gateway.security.service.AuthenticationService;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Extract LTPA token from JWT token and set it as a cookie when accessing z/OSMF
 *
 * @deprecated TODO: Remove after zowe-install-packaging is updated
 */
@Deprecated
public class ZosmfFilter extends ZuulFilter {
    private static final String ZOSMF = "zosmf";

    private static final String COOKIE_HEADER = "cookie";

    private final AuthenticationService authenticationService;

    @Autowired
    public ZosmfFilter(AuthenticationService tokenService) {
        this.authenticationService = tokenService;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext context = RequestContext.getCurrentContext();

        String serviceId = (String) context.get(SERVICE_ID_KEY);
        return StringUtils.containsIgnoreCase(serviceId, ZOSMF);
    }

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 4;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
        jwtToken.ifPresent(token -> {
            String ltpaToken = authenticationService.getLtpaTokenFromJwtToken(token);

            String cookie = context.getZuulRequestHeaders().get(COOKIE_HEADER);
            if (cookie != null) {
                cookie += "; " + ltpaToken;
            } else {
                cookie = ltpaToken;
            }

            context.addZuulRequestHeader(COOKIE_HEADER, cookie);
        });


        return null;
    }
}
