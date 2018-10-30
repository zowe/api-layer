/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.token;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;

public class TokenFilter extends AbstractSecureContentFilter {
    private final MFaaSConfigPropertiesContainer propertiesContainer;

    public TokenFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler,
                       MFaaSConfigPropertiesContainer propertiesContainer) {
        super(authenticationManager, failureHandler);
        this.propertiesContainer = propertiesContainer;
    }

    /**
     * Checks if token exists and extracts it
     *
     * @param request to check for header
     * @return token if it is present or null
     */
    @Override
    protected String extractContent(HttpServletRequest request) {
        String token = request.getHeader(propertiesContainer.getSecurity().getTokenProperties().getAuthorizationHeader());
        if (token == null) {
            return null;
        }
        if (token.startsWith(propertiesContainer.getSecurity().getTokenProperties().getBearerPrefix())) {
            return token.replaceFirst(propertiesContainer.getSecurity().getTokenProperties().getBearerPrefix(), "");
        }
        return null;
    }
}
