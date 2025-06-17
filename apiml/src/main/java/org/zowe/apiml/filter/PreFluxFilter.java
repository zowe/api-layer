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

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PreFluxFilter implements Filter {

    private final Type type;

    @Value("${apiml.service.port:10010}")
    private int externalPort;

    protected abstract void doFilterInternal(HttpServletRequest request, HttpServletResponse response,  FilterChain chain) throws IOException, ServletException;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (type.isApplicable(request, externalPort)) {
            doFilterInternal(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    public enum Type {
        INTERNAL_ONLY,
        EXTERNAL_ONLY,
        BOTH;

        boolean isApplicable(HttpServletRequest request, int externalPort) {
            switch (this) {
                case BOTH:
                    return true;
                case INTERNAL_ONLY:
                    return request.getServerPort() != externalPort;
                case EXTERNAL_ONLY:
                    return request.getServerPort() == externalPort;
                default:
                    return false;
            }
        }

    }

}
