/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.query;

import org.springframework.security.authentication.BadCredentialsException;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.InvalidCertificateException;
import org.zowe.apiml.security.common.error.InvalidTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Filter for /query endpoint requests with JWT token.
 */
public class QueryFilter extends AbstractAuthenticationProcessingFilter {
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final AuthenticationService authenticationService;
    private final HttpMethod httpMethod;
    private final boolean protectedByCertificate;

    public QueryFilter(
        String authEndpoint,
        AuthenticationSuccessHandler successHandler,
        AuthenticationFailureHandler failureHandler,
        AuthenticationService authenticationService,
        HttpMethod httpMethod,
        boolean protectedByCertificate,
        AuthenticationManager authenticationManager) {
        super(authEndpoint);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.authenticationService = authenticationService;
        this.httpMethod = httpMethod;
        this.protectedByCertificate = protectedByCertificate;
        this.setAuthenticationManager(authenticationManager);
    }

    /**
     * Calls authentication manager to validate the token
     *
     * @param request  the http request
     * @param response the http response
     * @return the authenticated token
     * @throws TokenNotProvidedException       when a token is not provided in the request
     * @throws AuthMethodNotSupportedException when the authentication method is not supported
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        if (!request.getMethod().equals(httpMethod.name())) {
            throw new AuthMethodNotSupportedException(request.getMethod());
        }

        // Must be already authenticated by certificate
        if (protectedByCertificate &&
            (SecurityContextHolder.getContext().getAuthentication() == null ||
                !(SecurityContextHolder.getContext().getAuthentication().getCredentials() instanceof X509Certificate) ||
                !SecurityContextHolder.getContext().getAuthentication().isAuthenticated())) {
            throw new InvalidCertificateException("Invalid certificate.");
        }

        String token = authenticationService.getJwtTokenFromRequest(request)
            .orElseThrow(() -> new TokenNotProvidedException("Authorization token not provided."));

        Authentication result = this.getAuthenticationManager().authenticate(new TokenAuthentication(token));
        if (result.isAuthenticated()) {
            return result;
        } else {
            throw new TokenNotValidException("JWT Token is not authenticated");
        }
    }

    /**
     * Calls successful query handler
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    /**
     * Calls unauthorized handler
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }
}
