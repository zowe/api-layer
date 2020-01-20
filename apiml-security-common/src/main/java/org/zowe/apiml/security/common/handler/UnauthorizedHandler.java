/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.handler;

import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles unauthorized access
 */
@Slf4j
@Component("plainAuth")
@RequiredArgsConstructor
public class UnauthorizedHandler implements AuthenticationEntryPoint {
    private final AuthExceptionHandler handler;

    /**
     * Creates unauthorized response with the appropriate message and http status
     *
     * @param request       the http request
     * @param response      the http response
     * @param authException the authorization exception
     * @throws ServletException when the response cannot be written
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws ServletException {
        log.debug("Unauthorized access to '{}' endpoint", request.getRequestURI());
        handler.handleException(request, response, authException);
    }
}
