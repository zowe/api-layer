/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.content;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.error.ResourceAccessExceptionHandler;
import com.ca.apiml.security.token.TokenAuthentication;
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
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public CookieContentFilter(AuthenticationManager authenticationManager,
                               AuthenticationFailureHandler failureHandler,
                               ResourceAccessExceptionHandler resourceAccessExceptionHandler,
                               SecurityConfigurationProperties securityConfigurationProperties) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, new String[0]);
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    public CookieContentFilter(AuthenticationManager authenticationManager,
                               AuthenticationFailureHandler failureHandler,
                               ResourceAccessExceptionHandler resourceAccessExceptionHandler,
                               SecurityConfigurationProperties securityConfigurationProperties,
                               String[] endpoints) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, endpoints);
        this.securityConfigurationProperties = securityConfigurationProperties;
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
            .filter(cookie -> cookie.getName().equals(securityConfigurationProperties.getCookieProperties().getCookieName()))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(cookie -> new TokenAuthentication(cookie.getValue()));
    }
}
