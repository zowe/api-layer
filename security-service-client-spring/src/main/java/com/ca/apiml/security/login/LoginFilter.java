/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.login;

import com.ca.apiml.security.error.AuthMethodNotSupportedException;
import com.ca.apiml.security.error.ResourceAccessExceptionHandler;
import com.ca.mfaas.constants.ApimlConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Filter to process authentication requests with the username and password in JSON format.
 */
@Slf4j
public class LoginFilter extends AbstractAuthenticationProcessingFilter {
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler;
    private final ObjectMapper mapper;

    public LoginFilter(
        String authEndpoint,
        AuthenticationSuccessHandler successHandler,
        AuthenticationFailureHandler failureHandler,
        ObjectMapper mapper,
        AuthenticationManager authenticationManager,
        ResourceAccessExceptionHandler resourceAccessExceptionHandler) {
        super(authEndpoint);
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.mapper = mapper;
        this.resourceAccessExceptionHandler = resourceAccessExceptionHandler;
        this.setAuthenticationManager(authenticationManager);
    }

    /**
     * Calls authentication manager to validate the username and password
     *
     * @param request  the http request
     * @param response the http response
     * @return the authenticated token
     * @throws AuthMethodNotSupportedException            when the authentication method is not supported
     * @throws AuthenticationCredentialsNotFoundException when username or password are not provided
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if (!request.getMethod().equals(HttpMethod.POST.name())) {
            throw new AuthMethodNotSupportedException(request.getMethod());
        }

        Optional<LoginRequest> optionalLoginRequest = getCredentialFromAuthorizationHeader(request);
        LoginRequest loginRequest = optionalLoginRequest.orElseGet(() -> getCredentialsFromBody(request));
        if (StringUtils.isBlank(loginRequest.getUsername()) || StringUtils.isBlank(loginRequest.getPassword())) {
            throw new AuthenticationCredentialsNotFoundException("Username or password not provided.");
        }

        UsernamePasswordAuthenticationToken authentication
            = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword());
        Authentication auth = null;
        try {
            auth = this.getAuthenticationManager().authenticate(authentication);
        } catch (RuntimeException ex) {
            resourceAccessExceptionHandler.handleException(request, response, ex);
        }
        return auth;
    }

    /**
     * Calls successful login handler
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

    /**
     * Extract credentials from the authorization header in the request and decode them
     *
     * @param request the http request
     * @return the decoded credentials
     */
    private Optional<LoginRequest> getCredentialFromAuthorizationHeader(HttpServletRequest request) {
        return Optional.ofNullable(
            request.getHeader(HttpHeaders.AUTHORIZATION)
        ).filter(
            header -> header.startsWith(ApimlConstants.BASIC_AUTHENTICATION_PREFIX)
        ).map(
            header -> header.replaceFirst(ApimlConstants.BASIC_AUTHENTICATION_PREFIX, "").trim()
        )
            .filter(base64Credentials -> !base64Credentials.isEmpty())
            .map(this::mapBase64Credentials);
    }

    /**
     * Decode the encoded credentials
     *
     * @param base64Credentials the credentials encoded in base64
     * @return the decoded credentials in {@link LoginRequest}
     */
    private LoginRequest mapBase64Credentials(String base64Credentials) {
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        int i = credentials.indexOf(':');
        if (i > 0) {
            return new LoginRequest(credentials.substring(0, i), credentials.substring(i + 1));
        }

        return null;
    }

    /**
     * Get credentials from the request body
     *
     * @param request the http request
     * @return the credentials in {@link LoginRequest}
     * @throws AuthenticationCredentialsNotFoundException if the login object has wrong format
     */
    private LoginRequest getCredentialsFromBody(HttpServletRequest request) {
        try {
            return mapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException e) {
            logger.debug("Authentication problem: login object has wrong format");
            throw new AuthenticationCredentialsNotFoundException("Login object has wrong format.");
        }
    }
}
