/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.handler;

import com.ca.apiml.security.exceptions.AuthMethodNotSupportedException;
import com.ca.apiml.security.query.TokenNotProvidedException;
import com.ca.apiml.security.token.TokenNotValidException;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Authentication error handler
 */
@Component
public class FailedAuthenticationHandler implements AuthenticationFailureHandler {
    private final ErrorService errorService;
    private final ObjectMapper mapper;

    public FailedAuthenticationHandler(ErrorService errorService, ObjectMapper objectMapper) {
        this.errorService = errorService;
        this.mapper = objectMapper;
    }

    /**
     * Handles authentication failure, decides on the exception type and selects appropriate message
     *
     * @param request   the http request
     * @param response  the http response
     * @param exception to be checked
     * @throws IOException when the response cannot be written
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        ApiMessage message;
        if (exception instanceof BadCredentialsException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            message = errorService.createApiMessage("apiml.security.login.invalidCredentials", request.getRequestURI());
        } else if (exception instanceof AuthenticationCredentialsNotFoundException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            message = errorService.createApiMessage("apiml.security.login.invalidInput", request.getRequestURI());
        } else if (exception instanceof AuthMethodNotSupportedException) {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
            message = errorService.createApiMessage("apiml.security.invalidMethod", exception.getMessage(), request.getRequestURI());
        } else if (exception instanceof TokenNotValidException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            message = errorService.createApiMessage("apiml.gateway.security.query.invalidToken", request.getRequestURI());
        } else if (exception instanceof TokenNotProvidedException) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            message = errorService.createApiMessage("apiml.gateway.security.query.tokenNotProvided", request.getRequestURI());
        } else {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            message = errorService.createApiMessage("apiml.security.generic", exception.getMessage(), request.getRequestURI());
        }

        mapper.writeValue(response.getWriter(), message);
    }
}
