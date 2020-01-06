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

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

import java.util.Optional;

import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.gateway.security.service.ServiceAuthenticationServiceImpl;
import com.ca.mfaas.gateway.security.service.schema.AuthenticationCommand;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This filter is responsible for customization request to clients from security
 * point of view. In this filter is fetched AuthenticationCommand which support
 * target security. In case it is possible decide now (all instances use the
 * same authentication) it will modify immediately. Otherwise in request params
 * will be set a command to load balancer. The request will be modified after
 * specific instance will be selected.
 */
public class ServiceAuthenticationFilter extends ZuulFilter {

    @Autowired
    private ServiceAuthenticationServiceImpl serviceAuthenticationService;

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 4;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        final RequestContext context = RequestContext.getCurrentContext();

        Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
        if (jwtToken.isPresent()) {
            final String serviceId = (String) context.get(SERVICE_ID_KEY);
            final AuthenticationCommand cmd = serviceAuthenticationService.getAuthenticationCommand(serviceId,
                    jwtToken.get());
            cmd.apply(null);
        }

        return null;
    }

}
