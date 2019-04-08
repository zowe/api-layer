/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.logout;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
@Component
public class SuccessfulLogoutHandler implements LogoutSuccessHandler {
    private final SecurityConfigurationProperties securityConfigurationProperties;

    @Autowired
    public SuccessfulLogoutHandler(SecurityConfigurationProperties securityConfigurationProperties) {
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
        HttpSession session = httpServletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);

        // Set the cookie to null and expired
        Cookie tokenCookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), null);
        tokenCookie.setPath(securityConfigurationProperties.getCookieProperties().getCookiePath());
        tokenCookie.setComment(securityConfigurationProperties.getCookieProperties().getCookieComment());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(0);
        httpServletResponse.addCookie(tokenCookie);

        // Clean the security context
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(null);
        SecurityContextHolder.clearContext();
    }
}
