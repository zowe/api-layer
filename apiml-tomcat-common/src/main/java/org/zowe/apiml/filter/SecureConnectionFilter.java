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

import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecureConnectionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (InboundAttls.get() != null && InboundAttls.get().getStatConn() == StatConn.SECURE) {
                filterChain.doFilter(request, response);
            } else {
                AttlsErrorHandler.handleError(response, "Inbound AT-TLS context is not initialized or connection is not secure." );
            }
        } catch (ContextIsNotInitializedException | UnknownEnumValueException | IoctlCallException | UnsatisfiedLinkError e) {
            logger.error("Can't read from AT-TLS context", e);
            AttlsErrorHandler.handleError(response, "Connection is not secure. " + e.getMessage());
        }
    }
}
