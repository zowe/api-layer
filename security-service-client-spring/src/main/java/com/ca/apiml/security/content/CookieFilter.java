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
import com.ca.apiml.security.token.TokenAuthentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

public class CookieFilter extends AbstractFilter {
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public CookieFilter(AuthenticationManager authenticationManager,
                        AuthenticationFailureHandler failureHandler,
                        SecurityConfigurationProperties securityConfigurationProperties) {
        super(authenticationManager, failureHandler);
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    /**
     * Extract  username and valid JWT token from the cookies
     * @param request the http request
     * @return the TokenAuthentication object containing username and valid JWT token
     */
    protected Optional<TokenAuthentication> extractContent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.asList(cookies)
            .stream()
            .filter(cookie -> cookie.getName().equals(securityConfigurationProperties.getCookieProperties().getCookieName()))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(cookie -> new TokenAuthentication(cookie.getValue()));
    }
}
