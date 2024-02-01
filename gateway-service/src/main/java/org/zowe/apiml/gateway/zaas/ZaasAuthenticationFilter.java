/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.zaas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import java.io.IOException;
import java.util.Optional;

import static org.zowe.apiml.gateway.filters.pre.ExtractAuthSourceFilter.AUTH_SOURCE_ATTR;

@RequiredArgsConstructor
public class ZaasAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSourceService authSourceService;
    private final AuthExceptionHandler authExceptionHandler;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Optional<AuthSource> authSource = Optional.ofNullable((AuthSource) request.getAttribute(AUTH_SOURCE_ATTR));
            if (!authSource.isPresent() || !authSourceService.isValid(authSource.get())) {
                throw new InsufficientAuthenticationException("Authentication failed.");
            }
            filterChain.doFilter(request, response);
        } catch (RuntimeException e) {
            authExceptionHandler.handleException(request, response, e);
        }
    }

}
