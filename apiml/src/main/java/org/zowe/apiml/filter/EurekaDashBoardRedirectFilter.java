/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * This filter is set to keep the Eureka homepage working in the same location as pre-modulith, since the main connector
 * has a mapping already to the Gateway homepage
 */
@Component
public class EurekaDashBoardRedirectFilter extends PreFluxFilter {

    public EurekaDashBoardRedirectFilter() {
        super(Type.INTERNAL_ONLY);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (StringUtils.equals(request.getRequestURI(), "/")) {
            request.getSession().getServletContext().getRequestDispatcher("/eureka").forward(request, response);
        } else {
            chain.doFilter(request, response);
        }

    }

}
