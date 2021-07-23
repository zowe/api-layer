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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecureConnectionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("z/os".equalsIgnoreCase(System.getProperty("os.name"))) {
            try {
                if (InboundAttls.getStatConn() != StatConn.SECURE) {
                    response.setStatus(500);
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.writeValue(response.getWriter(), "Connection is not secure");
                } else {
                    filterChain.doFilter(request, response);
                }
            } catch (ContextIsNotInitializedException | UnknownEnumValueException | IoctlCallException e) {
                throw new RuntimeException("Connection security can't be verified", e);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
