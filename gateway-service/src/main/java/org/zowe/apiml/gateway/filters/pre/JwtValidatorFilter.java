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
import lombok.RequiredArgsConstructor;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVLET_DETECTION_FILTER_ORDER;

/**
 * This is the very first filter in ZUUL. It verify JWT using on a call. In case of missing any JWT token it pass
 * through (non secure calls). Otherwise filter checks token validity. If JWT is valid it does nothing too. If
 * JWT is not valid or unsigned it returns response code 401 to user.
 */
@RequiredArgsConstructor
public class JwtValidatorFilter extends ZuulFilter {

    private final AuthenticationService authenticationService;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return SERVLET_DETECTION_FILTER_ORDER - 10;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        final RequestContext requestContext = RequestContext.getCurrentContext();

        final Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(requestContext.getRequest());
        if (jwtToken.isPresent()) {
            final TokenAuthentication tokenAuthentication = authenticationService.validateJwtToken(jwtToken.get());
            if (!tokenAuthentication.isAuthenticated()) {
                requestContext.setSendZuulResponse(false);
                requestContext.setResponseStatusCode(SC_UNAUTHORIZED);
            }
        }

        return null;
    }

}
