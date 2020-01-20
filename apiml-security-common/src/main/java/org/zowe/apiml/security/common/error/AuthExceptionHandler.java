/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.error;

import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Exception handler deals with exceptions (methods listed below) that are thrown during the authentication process
 */
@Slf4j
@Component
public class AuthExceptionHandler extends AbstractExceptionHandler {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AuthExceptionHandler(MessageService messageService, ObjectMapper objectMapper) {
        super(messageService, objectMapper);
    }

    /**
     * Entry method that takes care about the exception passed to it
     *
     * @param request  Http request
     * @param response Http response
     * @param ex       Exception to be handled
     * @throws ServletException Fallback exception if exception cannot be handled
     */
    @Override
    public void handleException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        if (ex instanceof InsufficientAuthenticationException) {
            handleAuthenticationRequired(request, response, ex);
        } else if (ex instanceof BadCredentialsException) {
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
    private void handleAuthenticationRequired(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_400, ex.getMessage());
        writeErrorResponse(ErrorType.AUTH_REQUIRED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, request, response);
    }

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
        final ApiMessageView message = messageService.createMessage(ErrorType.AUTH_METHOD_NOT_SUPPORTED.getErrorMessageKey(), ex.getMessage(), request.getRequestURI()).mapToView();
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

    //500
    private void handleAuthenticationException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        log.debug(ERROR_MESSAGE_500, ex.getMessage());
        log.debug("", ex);
        final ApiMessageView message = messageService.createMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), ex.getMessage(), request.getRequestURI()).mapToView();
        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        writeErrorResponse(message, status, response);
    }
}
