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
import com.netflix.zuul.exception.ZuulException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.zowe.apiml.gateway.filters.pre.SecFetchSitePolicy.REJECTION_MESSAGE;

/**
 * Applies {@link SecFetchSitePolicy} to proxied HTTP requests going through the Zuul pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecFetchSiteFilter extends PreZuulFilter {

    private final SecFetchSitePolicy secFetchSitePolicy;

    @Value("${apiml.security.csrf.preserveOriginForCrossSite:false}")
    private boolean preserveOriginForCrossSite;

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        HttpServletRequest request = context.getRequest();

        if (!secFetchSitePolicy.isAllowed(request::getHeader)) {
            context.setSendZuulResponse(false);
            context.setResponseBody(REJECTION_MESSAGE);
            context.addZuulResponseHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE);
            context.setResponseStatusCode(HttpStatus.FORBIDDEN.value());

            log.debug("Blocked cross-site {} {} - Sec-Fetch-Site={}, CORS is not enabled",
                request.getMethod(), request.getRequestURI(), request.getHeader(SecFetchSitePolicy.SEC_FETCH_SITE_HEADER));

            return null;
        }

        restoreOriginForCrossSiteRequest(context, request);

        return null;
    }

    /**
     * The gateway strips {@code Origin} from proxied requests by default (see {@code CorsBeans}), since it
     * is mainly relevant to the gateway's own CORS handling. When opted into via
     * {@code apiml.security.csrf.preserveOriginForCrossSite}, restore it for cross-site requests so a
     * southbound service that wants to apply its own CORS/Fetch-Metadata handling still sees it.
     */
    private void restoreOriginForCrossSiteRequest(RequestContext context, HttpServletRequest request) {
        if (!preserveOriginForCrossSite) {
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && secFetchSitePolicy.isCrossSite(request::getHeader)) {
            context.addZuulRequestHeader(HttpHeaders.ORIGIN, origin);
        }
    }

    @Override
    public int filterOrder() {
        return PRE_DECORATION_FILTER_ORDER + 9;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

}
