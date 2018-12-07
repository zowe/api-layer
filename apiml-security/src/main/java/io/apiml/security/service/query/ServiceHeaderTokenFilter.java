/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.query;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;

public class ServiceHeaderTokenFilter extends AbstractSecureContentFilter {
    public ServiceHeaderTokenFilter(
        AuthenticationManager authenticationManager,
        AuthenticationFailureHandler failureHandler) {
        super(authenticationManager, failureHandler);
    }

    @Override
    protected String extractContent(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            return token.replaceFirst("Bearer ", "");
        }
        return null;
    }
}
