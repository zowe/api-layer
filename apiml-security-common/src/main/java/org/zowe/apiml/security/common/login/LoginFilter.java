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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Filter to process authentication requests with the username and password in JSON format.
 */
public class LoginFilter extends NonCompulsoryAuthenticationProcessingFilter {
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

        Optional<LoginRequest> credentialFromHeader = getCredentialFromAuthorizationHeader(request);
        Optional<LoginRequest> credentialsFromBody = getCredentialsFromBody(request);
        LoginRequest loginRequest = credentialFromHeader.orElse(credentialsFromBody.orElse(null));
        return doAuth(request, response, loginRequest);

    }

    public Authentication doAuth(HttpServletRequest request, HttpServletResponse response, LoginRequest loginRequest) throws ServletException {

        if (loginRequest == null) {
            return null;
        }

        try {
            if (StringUtils.isBlank(loginRequest.getUsername()) || ArrayUtils.isEmpty(loginRequest.getPassword())) {
                throw new AuthenticationCredentialsNotFoundException("Username or password not provided.");
            }

            UsernamePasswordAuthenticationToken authentication
                = new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest);

            Authentication auth = null;

            try {
                auth = this.getAuthenticationManager().authenticate(authentication);
            } catch (RuntimeException ex) {
                resourceAccessExceptionHandler.handleException(request, response, ex);
            }
            return auth;
        } finally {
            loginRequest.evictSensitiveData();
        }
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
    public static Optional<LoginRequest> getCredentialFromAuthorizationHeader(HttpServletRequest request) {
        var headers = Optional.ofNullable(
            request.getHeader(HttpHeaders.AUTHORIZATION)
        );
        return getCredentialFromAuthorizationHeader(headers);
    }

    public static Optional<LoginRequest> getCredentialFromAuthorizationHeader(Optional<String> headers) {
        return headers.filter(
                header -> header.startsWith(ApimlConstants.BASIC_AUTHENTICATION_PREFIX)
            ).map(
                header -> header.replaceFirst(ApimlConstants.BASIC_AUTHENTICATION_PREFIX, "").trim()
            )
            .filter(base64Credentials -> !base64Credentials.isEmpty())
            .map(LoginFilter::mapBase64Credentials);
    }


    /**
     * Decode the encoded credentials
     *
     * @param base64Credentials the credentials encoded in base64
     * @return the decoded credentials in {@link LoginRequest}
     */
    private static LoginRequest mapBase64Credentials(String base64Credentials) {
        byte[] credentials = null;
        try {
            credentials = Base64.getDecoder().decode(base64Credentials);
            int index = ArrayUtils.indexOf(credentials, (byte) ':');
            if (index > 0) {
                byte[] password = null;
                char[] passwordChars;
                try {
                    password = Arrays.copyOfRange(credentials, index + 1, credentials.length);
                    passwordChars = new char[password.length];
                    for (int i = 0; i < password.length; i++) {
                        passwordChars[i] = (char) password[i];
                    }
                    return new LoginRequest(
                        new String(Arrays.copyOfRange(credentials, 0, index), StandardCharsets.UTF_8),
                        passwordChars
                    );
                } finally {
                    if (password != null) {
                        Arrays.fill(password, (byte) 0);
                    }
                }
            }
        } finally {
            if (credentials != null) {
                Arrays.fill(credentials, (byte) 0);
            }
        }
        throw new BadCredentialsException("Invalid basic authentication header");
    }

    /**
     * Get credentials from the request body
     *
     * @param request the http request
     * @return the credentials in {@link LoginRequest}
     * @throws AuthenticationCredentialsNotFoundException if the login object has wrong format
     */
    private Optional<LoginRequest> getCredentialsFromBody(HttpServletRequest request) {
        // method available could return 0 even there are some data, depends on the implementation
        try (
            var is = request.getInputStream();
            var bis = new BufferedInputStream(is)
        ) {
            if (is.isFinished()) {
                logger.trace("The input stream is already closed");
                return Optional.empty();
            }
            bis.mark(1);
            if (bis.read() < 0) {
                // no data available
                return Optional.empty();
            }
            // return to the beginning (to do not skip first character: '{')
            bis.reset();
            return Optional.of(mapper.readValue(bis, LoginRequest.class));
        } catch (IOException e) {
            logger.debug("Authentication problem: login object has wrong format");
            throw new AuthenticationCredentialsNotFoundException("Login object has wrong format.");
        }
    }
}
