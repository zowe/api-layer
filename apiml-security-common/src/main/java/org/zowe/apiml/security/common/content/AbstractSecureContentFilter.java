/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.content;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.handler.ServletErrorUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Filter base abstract class to secure application content
 */
@RequiredArgsConstructor
public abstract class AbstractSecureContentFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler failureHandler;
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler;
    private final String[] endpoints;
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (endpoints == null || endpoints.length == 0) {
            return false;
        }

        String path = request.getServletPath();
        return Arrays.stream(endpoints).noneMatch(path::startsWith);
    }

    /**
     * Extracts the token from the request
     *
     * @param request containing credentials
     * @return credentials
     */
    protected abstract Optional<AbstractAuthenticationToken> extractContent(HttpServletRequest request);

    /**
     * Extracts the token from the request and use the authentication manager to perform authentication.
     * Then set the currently authenticated principal and call the next filter in the chain.
     *
     * @param request     the http request
     * @param response    the http response
     * @param filterChain the filter chain
     * @throws ServletException a general exception
     * @throws IOException      a IO exception
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        Optional<AbstractAuthenticationToken> authenticationToken = extractContent(request);

        if (authenticationToken.isPresent()) {
            Authentication authentication = null;
            try {
                authentication = authenticationManager.authenticate(authenticationToken.get());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } catch (AuthenticationException authenticationException) {
                failureHandler.onAuthenticationFailure(request, response, authenticationException);
            } catch (RuntimeException e) {
                var consumer = ServletErrorUtils.createApiErrorWriter(response, apimlLog);
                var addHeader = (BiConsumer<String, String>) response::addHeader;
                resourceAccessExceptionHandler.handleException(request.getRequestURI(), consumer, addHeader, e);
            } finally {
                // TODO: remove once fixed directly in Spring - org.springframework.security.core.CredentialsContainer#eraseCredentials
                if (authentication != null) {
                    Object credentials = authentication.getCredentials();
                    if (credentials instanceof char[]) {
                        Arrays.fill((char[]) credentials, (char) 0);
                    }
                }
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
