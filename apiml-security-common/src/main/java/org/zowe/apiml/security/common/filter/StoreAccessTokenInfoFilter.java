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
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gateway.security.login.SuccessfulAccessTokenHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
/**
 * This filter will store the personal access information from the body as request attribute
 */
public class StoreAccessTokenInfoFilter extends OncePerRequestFilter {
    private static final String EXPIRATION_TIME = "expirationTime";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        ServletInputStream inputStream = request.getInputStream();
        if (inputStream.available() != 0) {
            int validity = new ObjectMapper().readValue(inputStream, SuccessfulAccessTokenHandler.AccessTokenRequest.class).getValidity();
            request.setAttribute(EXPIRATION_TIME, validity);
        }

        filterChain.doFilter(request, response);
    }
}
