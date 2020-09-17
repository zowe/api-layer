/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.post;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.post.SendErrorFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Custom {@link ZuulFilter} that replaces {@link SendErrorFilter}. Does not log errors to avoid
 * unnecessary log entries.
 */
@Component
public class CustomSendErrorFilter extends SendErrorFilter {
    public static final String ERROR_FILTER_RAN = SEND_ERROR_FILTER_RAN;

    private String errorPath;

    public CustomSendErrorFilter(@Value("${error.path:/error}") String errorPath) {
        this.errorPath = errorPath;
    }

    @Override
    public int filterOrder() {
        return super.filterOrder() - 1; // Ensure it runs before SendErrorFilter
    }

    @Override
    public Object run() {
        // Same behaviour as SendErrorFilter except for logging
        // This removes unhelpful logs, while still letting error controllers return informative responses
        try {
            RequestContext ctx = RequestContext.getCurrentContext();
            ExceptionHolder exception = findZuulException(ctx.getThrowable());
            HttpServletRequest request = ctx.getRequest();

            request.setAttribute("javax.servlet.error.status_code",
                exception.getStatusCode());

            request.setAttribute("javax.servlet.error.exception",
                exception.getThrowable());

            if (StringUtils.hasText(exception.getErrorCause())) {
                request.setAttribute("javax.servlet.error.message",
                    exception.getErrorCause());
            }

            RequestDispatcher dispatcher = request.getRequestDispatcher(this.errorPath);
            if (dispatcher != null) {
                ctx.set(SEND_ERROR_FILTER_RAN, true); // Disables other error filters
                if (!ctx.getResponse().isCommitted()) {
                    ctx.setResponseStatusCode(exception.getStatusCode());
                    dispatcher.forward(request, ctx.getResponse());
                }
            }
        } catch (Exception ex) {
            ReflectionUtils.rethrowRuntimeException(ex);
        }
        return null;
    }

    @Override
    public void setErrorPath(String errorPath) {
        this.errorPath = errorPath;
    }
}
