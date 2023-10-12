/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class ExtractAuthSourceFilter extends OncePerRequestFilter {
    static final String AUTH_SOURCE_ATTR = "zaas.auth.source";

    private final AuthSourceService authSourceService;
    private final AuthExceptionHandler authExceptionHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Optional<AuthSource> authSource = authSourceService.getAuthSourceFromRequest(request);
            if (authSource.isPresent()) {
                AuthSource.Parsed parsed = authSourceService.parse(authSource.get());
                request.setAttribute(AUTH_SOURCE_ATTR, parsed);
                filterChain.doFilter(request, response);
            } else {
                throw new InsufficientAuthenticationException("No authentication source found in the request.");
            }
        }
        catch (RuntimeException ex) {
            authExceptionHandler.handleException(request, response, ex);
        }

    }
}
