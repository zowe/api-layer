/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.error;

import com.ca.apiml.security.token.TokenExpireException;
import com.ca.apiml.security.token.TokenNotProvidedException;
import com.ca.apiml.security.token.TokenNotValidException;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class AuthExceptionHandler {

    private static final String ERROR_MESSAGE_400 = "400 Status Code: {}";
    private static final String ERROR_MESSAGE_500 = "500 Status Code";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;

    private final ErrorService errorService;
    private final ObjectMapper mapper;

    public AuthExceptionHandler(ErrorService errorService, ObjectMapper objectMapper) {
        this.errorService = errorService;
        this.mapper = objectMapper;
    }

    public void handleAuthException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        if (ex instanceof BadCredentialsException
            || ex instanceof InsufficientAuthenticationException) {
            handleBadCredentials(request, response, ex);
        } else if (ex instanceof AuthenticationCredentialsNotFoundException) {
            handleAuthenticationCredentialsNotFound(request, response, ex);
        } else if (ex instanceof AuthMethodNotSupportedException) {
            handleAuthMethodNotSupported(request, response, ex);
        } else if (ex instanceof TokenNotValidException) {
            handleTokenNotValid(request, response, ex);
        } else if (ex instanceof TokenNotProvidedException) {
            handleTokenNotProvided(request, response, ex);
        } else if (ex instanceof TokenExpireException) {
            handleTokenExpire(request, response, ex);
        } else if (ex instanceof AuthenticationException) {
            handleAuthenticationException(request, response, ex);
        } else {
            throw new ServletException(ex);
        }
    }

    // 400
    private void handleBadCredentials(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        writeErrorResponse(ErrorType.BAD_CREDENTIALS.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, request, response);
    }

    private void handleAuthenticationCredentialsNotFound(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        writeErrorResponse(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getErrorMessageKey(), HttpStatus.BAD_REQUEST, request, response);
    }

    private void handleAuthMethodNotSupported(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        final ApiMessage message = errorService.createApiMessage(ErrorType.AUTH_METHOD_NOT_SUPPORTED.getErrorMessageKey(), ex.getMessage(), request.getRequestURI());
        final HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        writeErrorResponse(message, status, response);
    }

    private void handleTokenNotValid(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, request, response);
    }

    private void handleTokenNotProvided(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_NOT_PROVIDED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, request, response);
    }

    private void handleTokenExpire(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_EXPIRED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, request, response);
    }

    private void handleAuthenticationException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.error(ERROR_MESSAGE_500, ex);
        final ApiMessage message = errorService.createApiMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), ex.getMessage(), request.getRequestURI());
        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        writeErrorResponse(message, status, response);
    }

    private void writeErrorResponse(String messageKey, HttpStatus status, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        final ApiMessage message = errorService.createApiMessage(messageKey, request.getRequestURI());
        writeErrorResponse(message, status, response);
    }

    private void writeErrorResponse(ApiMessage message, HttpStatus status, HttpServletResponse response) throws ServletException {
        response.setStatus(status.value());
        response.setContentType(CONTENT_TYPE);
        try {
            mapper.writeValue(response.getWriter(), message);
        } catch (IOException e) {
            String errorMessage = "Couldn't write response";
            log.error(errorMessage, e);
            throw new ServletException(errorMessage, e);
        }
    }
}
