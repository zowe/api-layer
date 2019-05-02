/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.query;

import com.ca.mfaas.gateway.security.AuthMethodNotSupportedException;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.apiml.security.token.TokenAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Filter for query endpoint request with JWT token.
 */
@Slf4j
public class QueryFilter extends AbstractAuthenticationProcessingFilter {
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final AuthenticationService authenticationService;

    public QueryFilter(
        String authEndpoint,
        AuthenticationSuccessHandler successHandler,
        AuthenticationFailureHandler failureHandler,
        AuthenticationService authenticationService,
        AuthenticationManager authenticationManager) {
        super(authEndpoint);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.authenticationService = authenticationService;
        this.setAuthenticationManager(authenticationManager);
    }

    /**
     * Attempt authentication and return fully populated Authentication object
     *
     * @param request
     * @param response
     * @throws TokenNotProvidedException when token is not provided in request
     * @throws AuthMethodNotSupportedException when trying with other method than GET
     * @return Authentication object on successful authentication
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (!request.getMethod().equals(HttpMethod.GET.name())) {
            throw new AuthMethodNotSupportedException(request.getMethod());
        }

        String token = authenticationService.getJwtTokenFromRequest(request)
            .orElseThrow(() -> new TokenNotProvidedException("Authorization token not provided."));

        return this.getAuthenticationManager().authenticate(new TokenAuthentication(token));
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}

