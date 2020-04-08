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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.ServiceAuthenticationServiceImpl;
import org.zowe.apiml.gateway.security.service.schema.AuthenticationCommand;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.*;

/**
 * This filter is responsible for customization request to clients from security point of view. In this filter is
 * fetched AuthenticationCommand which support target security. In case it is possible decide now (all instances
 * use the same authentication) it will modify immediately. Otherwise in request params will be set a command to
 * load balancer. The request will be modified after specific instance will be selected.
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
        return PRE_DECORATION_FILTER_ORDER + 5;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        final RequestContext context = RequestContext.getCurrentContext();

        boolean rejected = false;
        AuthenticationCommand cmd = null;

        final String serviceId = (String) context.get(SERVICE_ID_KEY);
        try {
            String jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest()).orElse(null);
            cmd = serviceAuthenticationService.getAuthenticationCommand(serviceId, jwtToken);

            // Verify JWT validity if it is required for the schema
            if ((jwtToken != null) && cmd.isRequiredValidJwt()) {
                if (!authenticationService.validateJwtToken(jwtToken).isAuthenticated()) {
                    rejected = true;
                }
            }
        } catch (AuthenticationException ae) {
            rejected = true;
        } catch (Exception e) {
            throw new ZuulRuntimeException(
                new ZuulException(e, HttpStatus.INTERNAL_SERVER_ERROR.value(), String.valueOf(e))
            );
        }

        if (rejected) {
            context.setSendZuulResponse(false);
            context.setResponseStatusCode(SC_UNAUTHORIZED);
            return null;
        } else {
            try {
                // Update ZUUL context by authentication schema
                cmd.apply(null);
            } catch (Exception e) {
                throw new ZuulRuntimeException(
                    new ZuulException(e, HttpStatus.INTERNAL_SERVER_ERROR.value(), String.valueOf(e))
                );
            }
        }

        return null;
    }

}
