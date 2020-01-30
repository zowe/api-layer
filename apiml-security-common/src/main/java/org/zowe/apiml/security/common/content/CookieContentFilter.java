/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.content;

import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

/**
 * Authenticate the JWT token stored in the cookie
 */
public class CookieContentFilter extends AbstractSecureContentFilter {
    private final AuthConfigurationProperties authConfigurationProperties;

    public CookieContentFilter(AuthenticationManager authenticationManager,
                               AuthenticationFailureHandler failureHandler,
                               ResourceAccessExceptionHandler resourceAccessExceptionHandler,
                               AuthConfigurationProperties authConfigurationProperties) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, new String[0]);
        this.authConfigurationProperties = authConfigurationProperties;
    }

    public CookieContentFilter(AuthenticationManager authenticationManager,
                               AuthenticationFailureHandler failureHandler,
                               ResourceAccessExceptionHandler resourceAccessExceptionHandler,
                               AuthConfigurationProperties authConfigurationProperties,
                               String[] endpoints) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, endpoints);
        this.authConfigurationProperties = authConfigurationProperties;
    }

    /**
     * Extract the valid JWT token from the cookies
     *
     * @param request the http request
     * @return the {@link TokenAuthentication} object containing username and valid JWT token
     */
    public Optional<AbstractAuthenticationToken> extractContent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(authConfigurationProperties.getCookieProperties().getCookieName()))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(cookie -> new TokenAuthentication(cookie.getValue()));
    }
}
