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

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class BasicFilter extends OncePerRequestFilter {
    private static final String BASIC_AUTHENTICATION_PREFIX = "Basic ";

    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;

    public BasicFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler) {
        this.authenticationManager = authenticationManager;
        this.failureHandler = failureHandler;
    }

    /**
     * Extract the token from the request and use the authentication manager to perform authentication.
     * Then set the currently authenticated principal and call the next filter in the chain
     * @param request the http request
     * @param response the http response
     * @param filterChain the filter chain
     * @throws ServletException
     * @throws  IOException
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        Optional<UsernamePasswordAuthenticationToken> authenticationToken = extractContent(request);

        if (authenticationToken.isPresent()) {
            try {
                Authentication authentication = authenticationManager.authenticate(authenticationToken.get());
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
     * Extract credentials from the authorization header in the request and decode them
     * @param request the http request
     * @return the decoded credentials
     */
    private Optional<UsernamePasswordAuthenticationToken> extractContent(HttpServletRequest request) {
        return Optional.ofNullable(
            request.getHeader(HttpHeaders.AUTHORIZATION)
        ).filter(
            header -> header.startsWith(BASIC_AUTHENTICATION_PREFIX)
        ).map(
            header -> header.replaceFirst(BASIC_AUTHENTICATION_PREFIX, "")
        )
         .filter(base64Credentials -> !base64Credentials.isEmpty())
         .map(this::mapBase64Credentials);
    }

    /**
     * Decode the encoded credentials
     * @param base64Credentials the credentials encoded in base64
     * @return the decoded credentials
     */
    private UsernamePasswordAuthenticationToken mapBase64Credentials(String base64Credentials) {
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        int i = credentials.indexOf(':');
        if (i > 0) {
            return new UsernamePasswordAuthenticationToken(credentials.substring(0, i), credentials.substring(i + 1));
        }

        return null;
    }
}
