/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.handler.ServletErrorUtils;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.zowe.apiml.zaas.zaas.ExtractAuthSourceFilter.AUTH_SOURCE_ATTR;
import static org.zowe.apiml.zaas.zaas.ExtractAuthSourceFilter.AUTH_SOURCE_PARSED_ATTR;

@RequiredArgsConstructor
public class ZaasAuthenticationFilter extends OncePerRequestFilter {

    private final AuthSourceService authSourceService;
    private final AuthExceptionHandler authExceptionHandler;
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Optional<AuthSource> authSource = Optional.ofNullable((AuthSource) request.getAttribute(AUTH_SOURCE_ATTR));
            if (authSource.isEmpty() || !authSourceService.isValid(authSource.get())) {
                throw new InsufficientAuthenticationException("Authentication failed.");
            }

            Optional<AuthSource.Parsed> authSourceParsed = Optional.ofNullable((AuthSource.Parsed) request.getAttribute(AUTH_SOURCE_PARSED_ATTR));
            if (authSourceParsed.isEmpty() || StringUtils.isBlank(authSourceParsed.get().getUserId())) {
                throw new InsufficientAuthenticationException("Authentication failed.");
            }

            filterChain.doFilter(request, response);
        } catch (RuntimeException e) {
            var consumer = ServletErrorUtils.createApiErrorWriter(response, apimlLog);
            var addHeader = (BiConsumer<String, String>) response::addHeader;
            authExceptionHandler.handleException(request.getRequestURI(), consumer, addHeader, e);
        }
    }

}
