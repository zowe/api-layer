/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.security;

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Handles logout success by removing cookie and clearing security context
 */
@RequiredArgsConstructor
public class ApiCatalogLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final AuthConfigurationProperties authConfigurationProperties;

    /**
     * Clears cookie, session, context and sets response code
     *
     * @param httpServletRequest Http request
     * @param httpServletResponse Http response
     * @param authentication Valid authentication
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                Authentication authentication) {
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);

        // Set the cookie to null and expired
        Cookie tokenCookie = new Cookie(authConfigurationProperties.getCookieProperties().getCookieName(), null);
        tokenCookie.setPath(authConfigurationProperties.getCookieProperties().getCookiePath());
        tokenCookie.setComment(authConfigurationProperties.getCookieProperties().getCookieComment());
        tokenCookie.setSecure(true);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(0);
        httpServletResponse.addCookie(tokenCookie);

        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(null);
        SecurityContextHolder.clearContext();
    }
}
