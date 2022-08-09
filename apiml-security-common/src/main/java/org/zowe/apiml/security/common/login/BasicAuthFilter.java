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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class BasicAuthFilter extends LoginFilter {

    public BasicAuthFilter(String authEndpoint, AuthenticationFailureHandler failureHandler, ObjectMapper mapper, AuthenticationManager authenticationManager, ResourceAccessExceptionHandler resourceAccessExceptionHandler) {
//no need for success handler implementation, we just need to continue in process chain, this is the reason for lambda rather than pass null
        super(authEndpoint, ((request, response, authentication) -> {
        }), failureHandler, mapper, authenticationManager, resourceAccessExceptionHandler);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Optional<LoginRequest> credentialFromHeader = LoginFilter.getCredentialFromAuthorizationHeader(request);
        LoginRequest loginRequest = credentialFromHeader.orElse(null);
        return doAuth(request, response, loginRequest);
    }

    /**
     * Calls successful login handler
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);
        chain.doFilter(request, response);
    }
}
