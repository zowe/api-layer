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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.token.*;

import java.util.function.BiConsumer;

/**
 * Exception handler deals with exceptions (methods listed below) that are thrown during the authentication process
 */
@Slf4j
@Component
public class AuthExceptionHandler extends AbstractExceptionHandler {

    private final boolean isModulith;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public AuthExceptionHandler(
        MessageService messageService,
        ObjectMapper objectMapper,
        @Autowired(required = false) @Qualifier("isModulith") Boolean isModulith) {
        super(messageService, objectMapper);
        this.isModulith = Boolean.TRUE.equals(isModulith);
    }

    /**
     * Entry method that takes care about the exception passed to it
     *
     * @param requestUri Http request URI
     * @param function message function
     * @param addHeader header
     * @param ex Exception to be handled
     */
    @Override
    public void handleException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, RuntimeException ex) throws ServletException {
        if (ex instanceof InsufficientAuthenticationException) {
            handleAuthenticationRequired(requestUri, function, addHeader, ex);
        } else if (ex instanceof BadCredentialsException) {
            handleBadCredentials(requestUri, function, ex);
        } else if (ex instanceof AuthenticationCredentialsNotFoundException) {
            handleAuthenticationCredentialsNotFound(requestUri, function, ex);
        } else if (ex instanceof AuthMethodNotSupportedException) {
            handleAuthMethodNotSupported(requestUri, function, ex);
        } else if (ex instanceof TokenNotValidException) {
            handleTokenNotValid(requestUri, function, addHeader, ex);
        } else if (ex instanceof NoMainframeIdentityException nmie) {
            handleNoMainframeIdentity(requestUri, function, addHeader, nmie);
        } else if (ex instanceof TokenNotProvidedException) {
            handleTokenNotProvided(requestUri, function, ex);
        } else if (ex instanceof TokenExpireException) {
            handleTokenExpire(requestUri, function, ex);
        } else if (ex instanceof TokenFormatNotValidException) {
            handleTokenFormatException(requestUri, function, ex);
        } else if (ex instanceof AccessTokenBodyNotValidException) {
            handleInvalidAccessTokenBodyException(requestUri, function, ex);
        } else if (ex instanceof InvalidCertificateException) {
            handleInvalidCertificate(function, ex);
        } else if (ex instanceof ZosAuthenticationException) {
            handleZosAuthenticationException(function, (ZosAuthenticationException) ex);
        } else if (ex instanceof InvalidTokenTypeException) {
            handleInvalidTokenTypeException(requestUri, function, ex);
        } else if (ex instanceof AuthenticationException) {
            handleAuthenticationException(requestUri, function, ex);
        } else if (ex instanceof ServiceNotAccessibleException) {
            handleServiceNotAccessibleException(requestUri, function, ex);
        } else {
            if (!isModulith) {
                throw new ServletException(ex);
            }
            handleAuthenticationException(requestUri, function, ex);
        }
    }

    private void handleZosAuthenticationException(BiConsumer<ApiMessageView, HttpStatus> function, ZosAuthenticationException ex) {
        final ApiMessageView message = messageService.createMessage(ex.getPlatformError().errorMessage, ex.getMessage()).mapToView();
        final HttpStatus status = ex.getPlatformError().responseCode;
        function.accept(message, status);
    }

    private void handleAuthenticationRequired(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        String error = this.messageService.createMessage("org.zowe.apiml.zaas.security.schema.missingAuthentication").mapToLogMessage();
        addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, error);
        writeErrorResponse(ErrorType.AUTH_REQUIRED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleBadCredentials(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.BAD_CREDENTIALS.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleAuthenticationCredentialsNotFound(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        writeErrorResponse(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getErrorMessageKey(), HttpStatus.BAD_REQUEST, requestUri, function);
    }

    private void handleAuthMethodNotSupported(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        final HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        final ApiMessageView message = messageService.createMessage(ErrorType.AUTH_METHOD_NOT_SUPPORTED.getErrorMessageKey(), ex.getMessage(), requestUri).mapToView();
        function.accept(message, status);
    }

    private void handleTokenNotValid(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        String error = this.messageService.createMessage("org.zowe.apiml.common.unauthorized").mapToLogMessage();
        addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, error);
        writeErrorResponse(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleNoMainframeIdentity(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, NoMainframeIdentityException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        addHeader.accept(ApimlConstants.HEADER_OIDC_TOKEN, ex.getToken());
        addHeader.accept(ApimlConstants.AUTH_FAIL_HEADER, ex.getMessage());
        writeErrorResponse(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleTokenNotProvided(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_NOT_PROVIDED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleTokenExpire(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_EXPIRED.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleInvalidCertificate(BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        function.accept(null, HttpStatus.FORBIDDEN);
        log.debug(MESSAGE_FORMAT, HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    private void handleTokenFormatException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        writeErrorResponse(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), HttpStatus.BAD_REQUEST, requestUri, function);
    }

    private void handleInvalidTokenTypeException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        writeErrorResponse(ErrorType.INVALID_TOKEN_TYPE.getErrorMessageKey(), HttpStatus.UNAUTHORIZED, requestUri, function);
    }

    private void handleInvalidAccessTokenBodyException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        log.debug(MESSAGE_FORMAT, HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        writeErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, requestUri, function);
    }

    private void handleAuthenticationException(String uri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        final ApiMessageView message = messageService.createMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), ex.getMessage(), uri).mapToView();
        final HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        function.accept(message, status);
    }

    private void handleServiceNotAccessibleException(String uri, BiConsumer<ApiMessageView, HttpStatus> function, RuntimeException ex) {
        final ApiMessageView message = messageService.createMessage(ErrorType.SERVICE_UNAVAILABLE.getErrorMessageKey(), ex.getMessage(), uri).mapToView();
        final HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        log.debug(MESSAGE_FORMAT, status.value(), ex.getMessage());
        function.accept(message, status);
    }
}
