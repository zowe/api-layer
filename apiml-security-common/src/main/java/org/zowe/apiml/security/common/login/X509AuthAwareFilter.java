/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.io.IOException;

public class X509AuthAwareFilter extends X509AuthenticationFilter {
    private final AuthenticationFailureHandler failureHandler;

    public X509AuthAwareFilter(String endpoint, AuthenticationFailureHandler failureHandler, AuthenticationProvider authenticationProvider) {
        //no need for success handler implementation, we just need to continue in process chain, this is the reason for lambda rather than pass null
        super(endpoint, ((request, response, authentication) -> {
        }), authenticationProvider);
        this.failureHandler = failureHandler;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        // TODO: investigate how to improve the security config to avoid calling isPreAuthenticated()
        if (SecurityContextHolder.getContext().getAuthentication() == null || isPreAuthenticated() || !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authResult);
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }

    protected boolean isPreAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof PreAuthenticatedAuthenticationToken;
    }
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
