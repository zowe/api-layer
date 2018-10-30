/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.token;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;

public class HeaderTokenFilter extends AbstractTokenFilter {
    private static final String HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    public HeaderTokenFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler) {
        super(authenticationManager, failureHandler);
    }

    /**
     * Checks if token exists and extracts it
     * @param request to check for header
     * @return token if it is present or null
     */
    @Override
    protected String extractToken(HttpServletRequest request) {
        String token = request.getHeader(HEADER);
        if (token == null) {
            return null;
        }
        if (token.startsWith(TOKEN_PREFIX)) {
            return token.replaceFirst(TOKEN_PREFIX, "");
        }
        return null;
    }
}
