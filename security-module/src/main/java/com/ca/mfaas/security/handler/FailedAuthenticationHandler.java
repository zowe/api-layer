/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.handler;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import com.ca.mfaas.security.login.InvalidUserException;
import com.ca.mfaas.security.token.TokenExpireException;
import com.ca.mfaas.security.token.TokenNotValidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class FailedAuthenticationHandler implements AuthenticationFailureHandler {
    private final ErrorService errorService;
    private final ObjectMapper mapper;

    public FailedAuthenticationHandler(@Qualifier("securityErrorService") ErrorService errorService,
                                       @Qualifier("securityObjectMapper") ObjectMapper objectMapper) {
        this.errorService = errorService;
        this.mapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        ApiMessage message = null;
        if (exception instanceof TokenExpireException) {
            message = errorService.createApiMessage("com.ca.mfaas.security.tokenIsExpired", exception.getMessage(), request.getRequestURI());
        } else if (exception instanceof TokenNotValidException) {
            message = errorService.createApiMessage("com.ca.mfaas.security.tokenIsNotValid", exception.getMessage(), request.getRequestURI());
        } else if (exception instanceof InvalidUserException) {
            message = errorService.createApiMessage("com.ca.mfaas.security.invalidUsername", exception.getMessage(), request.getRequestURI());
        } else {
            message = errorService.createApiMessage("com.ca.mfaas.security.authenticationException", exception.getMessage(), request.getRequestURI());
        }
        mapper.writeValue(response.getWriter(), message);
    }
}
