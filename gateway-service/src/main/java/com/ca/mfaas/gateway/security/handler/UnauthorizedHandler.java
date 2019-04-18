/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.handler;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.product.constants.ApimConstants;
import com.ca.mfaas.rest.response.ApiMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
@Component
public class UnauthorizedHandler implements AuthenticationEntryPoint {
    private final ErrorService errorService;

    public UnauthorizedHandler(ErrorService errorService) {
        this.errorService = errorService;
    }

    /**
     * Handles authentication errors for endpoints access
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.debug("Unauthorized access to '{}' endpoint", request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.addHeader("WWW-Authenticate", ApimConstants.BASIC_AUTHENTICATION_PREFIX);

        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.gateway.security.invalidCredentials", request.getRequestURI());
        PrintWriter writer = response.getWriter();
        writer.println("HTTP Status 401 : " + message.toString());
    }
}
