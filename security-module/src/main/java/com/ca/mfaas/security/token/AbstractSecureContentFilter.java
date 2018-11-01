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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public abstract class AbstractSecureContentFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;

    AbstractSecureContentFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler) {
        this.authenticationManager = authenticationManager;
        this.failureHandler = failureHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractContent(request);
        if (token != null) {
            TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
            try {
                Authentication authentication = authenticationManager.authenticate(tokenAuthentication);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } catch (AuthenticationException authenticationException) {
                failureHandler.onAuthenticationFailure(request, response, authenticationException);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Checks if token exists and extracts it
     *
     * @param request to check
     * @return token if it is present or null
     */
    protected abstract String extractContent(HttpServletRequest request);
}
