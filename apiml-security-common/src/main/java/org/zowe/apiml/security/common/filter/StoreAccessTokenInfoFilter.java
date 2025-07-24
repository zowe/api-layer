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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.AccessTokenInvalidBodyException;
import org.zowe.apiml.security.common.error.AccessTokenMissingBodyException;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.handler.ServletErrorUtils;
import org.zowe.apiml.security.common.handler.SuccessfulAccessTokenHandler;

import java.io.IOException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * This filter will store the personal access information from the body as request attribute
 */
@RequiredArgsConstructor
public class StoreAccessTokenInfoFilter extends OncePerRequestFilter {
    public static final String TOKEN_REQUEST = "tokenRequest";

    private static final ObjectReader mapper = new ObjectMapper().reader();

    private final AuthExceptionHandler authExceptionHandler;
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException {
        var consumer = ServletErrorUtils.createApiErrorWriter(response, apimlLog);
        var addHeader = (BiConsumer<String, String>) response::addHeader;
        try {
            ServletInputStream inputStream = request.getInputStream();
            if (inputStream.available() != 0) {
                SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = mapper.readValue(inputStream, SuccessfulAccessTokenHandler.AccessTokenRequest.class);
                Set<String> scopes = accessTokenRequest.getScopes();
                if (scopes == null || scopes.isEmpty()) {
                    authExceptionHandler.handleException(request.getRequestURI(), consumer, addHeader, new AccessTokenMissingBodyException("Scopes are missing in the request."));
                    return;
                }
                accessTokenRequest.setScopes(scopes.stream().map(String::toLowerCase).collect(Collectors.toSet()));
                request.setAttribute(TOKEN_REQUEST, accessTokenRequest);
                filterChain.doFilter(request, response);
            } else {
                authExceptionHandler.handleException(request.getRequestURI(), consumer, addHeader, new AccessTokenMissingBodyException("Body is missing in the request."));
            }

        } catch (IOException e) {
            authExceptionHandler.handleException(request.getRequestURI(), consumer, addHeader, new AccessTokenInvalidBodyException("Format of the request body is invalid. Exception message: " + e.getMessage()));
        }

    }

}
