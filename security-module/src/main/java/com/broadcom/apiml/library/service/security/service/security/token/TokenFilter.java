/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.security.token;

import com.broadcom.apiml.library.service.security.service.security.config.SecurityConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;

public class TokenFilter extends AbstractSecureContentFilter {
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public TokenFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler,
                       SecurityConfigurationProperties securityConfigurationProperties) {
        super(authenticationManager, failureHandler);
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    /**
     * Checks if token exists and extracts it
     *
     * @param request to check for header
     * @return token if it is present or null
     */
    @Override
    protected String extractContent(HttpServletRequest request) {
        String token = request.getHeader(securityConfigurationProperties.getTokenProperties().getAuthorizationHeader());
        if (token == null) {
            return null;
        }
        if (token.startsWith(securityConfigurationProperties.getTokenProperties().getBearerPrefix())) {
            return token.replaceFirst(securityConfigurationProperties.getTokenProperties().getBearerPrefix(), "");
        }
        return null;
    }
}
