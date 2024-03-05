/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.servlet;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

@Component
@WebFilter
public class ServletContextUpdateFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (StringUtils.isEmpty(request.getServletContext().getContextPath())) {
            request = new ServletRequestWrapper(request) {
                @Override
                public ServletContext getServletContext() {
                    return new ServletContextWrapper(super.getServletContext()) {
                        @Override
                        public String getContextPath() {
                            return "/";
                        }
                    };
                }
            };
        }

        chain.doFilter(request, response);
    }

    @RequiredArgsConstructor
    static class ServletContextWrapper implements ServletContext {

        @Delegate
        private final ServletContext servletContext;

    }

}
