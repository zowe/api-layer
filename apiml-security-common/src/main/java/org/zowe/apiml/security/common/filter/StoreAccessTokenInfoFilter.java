/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gateway.security.login.SuccessfulAccessTokenHandler;
import org.zowe.apiml.security.common.error.AccessTokenBodyNotValidException;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This filter will store the personal access information from the body as request attribute
 */
@RequiredArgsConstructor
public class StoreAccessTokenInfoFilter extends OncePerRequestFilter {
    public static final String TOKEN_REQUEST = "tokenRequest";
    private static final ObjectReader mapper = new ObjectMapper().reader();
    private final AuthExceptionHandler authExceptionHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException {
        try {
            ServletInputStream inputStream = request.getInputStream();
            if (inputStream.available() != 0) {
                SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = mapper.readValue(inputStream, SuccessfulAccessTokenHandler.AccessTokenRequest.class);
                Set<String> scopes = accessTokenRequest.getScopes();
                if (scopes == null || scopes.isEmpty()) {
                    authExceptionHandler.handleException(request, response,  new AccessTokenBodyNotValidException("org.zowe.apiml.security.token.accessTokenBodyMissingScopes"));
                    return;
                }
                accessTokenRequest.setScopes(scopes.stream().map(String::toLowerCase).collect(Collectors.toSet()));
                request.setAttribute(TOKEN_REQUEST, accessTokenRequest);
                filterChain.doFilter(request, response);
            } else {
                authExceptionHandler.handleException(request, response,  new AccessTokenBodyNotValidException("org.zowe.apiml.security.token.accessTokenBodyMissingScopes"));
            }

        } catch (IOException e) {
            authExceptionHandler.handleException(request, response, new AccessTokenBodyNotValidException("org.zowe.apiml.security.query.invalidAccessTokenBody"));
        }
    }
}
