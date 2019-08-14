/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.client.handler;

import com.ca.apiml.security.common.error.AuthMethodNotSupportedException;
import com.ca.apiml.security.common.error.ErrorType;
import com.ca.apiml.security.common.error.ServiceNotAccessibleException;
import com.ca.apiml.security.common.token.TokenNotProvidedException;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.product.gateway.GatewayNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import javax.validation.constraints.NotNull;

/**
 * Handler for exceptions that are thrown during the security client rest calls
 */
@Slf4j
@Component
public class RestResponseHandler {

    /**
     * Consumes an exception and transforms it into manageable exception
     *
     * @param exception Input exception, can not be null
     * @param errorType Error type enum, see {@link ErrorType}
     * @param genericLogErrorMessage Generic message that gets printed in log
     * @param logParameters Additional messages are printed into the log after the generic message log line
     */
    public void handleBadResponse(@NotNull Exception exception, ErrorType errorType, String genericLogErrorMessage, Object... logParameters) {
        if (exception instanceof HttpClientErrorException) {
            handleHttpClientError(exception, errorType, genericLogErrorMessage, logParameters);
        } else if (exception instanceof ResourceAccessException) {
            throw new GatewayNotFoundException(ErrorType.GATEWAY_NOT_FOUND.getDefaultMessage(), exception);
        } else if (exception instanceof HttpServerErrorException) {
            HttpServerErrorException hseException = (HttpServerErrorException) exception;
            if (hseException.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                throw new ServiceNotAccessibleException(ErrorType.SERVICE_UNAVAILABLE.getDefaultMessage(), exception);
            } else {
                throw hseException;
            }
        }
    }

    private void handleHttpClientError(@NotNull Exception exception, ErrorType errorType, String genericLogErrorMessage, Object... logParameters) {
        HttpClientErrorException hceException = (HttpClientErrorException) exception;
        switch (hceException.getStatusCode()) {
            case UNAUTHORIZED:
                if (errorType != null) {
                    if (errorType.equals(ErrorType.BAD_CREDENTIALS)) {
                        throw new BadCredentialsException(errorType.getDefaultMessage(), exception);
                    } else if (errorType.equals(ErrorType.TOKEN_NOT_VALID)) {
                        throw new TokenNotValidException(errorType.getDefaultMessage(), exception);
                    } else if (errorType.equals(ErrorType.TOKEN_NOT_PROVIDED)) {
                        throw new TokenNotProvidedException(errorType.getDefaultMessage());
                    }
                }
                throw new BadCredentialsException(ErrorType.BAD_CREDENTIALS.getDefaultMessage(), exception);
            case BAD_REQUEST:
                throw new AuthenticationCredentialsNotFoundException(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getDefaultMessage(), exception);
            case METHOD_NOT_ALLOWED:
                throw new AuthMethodNotSupportedException(ErrorType.AUTH_METHOD_NOT_SUPPORTED.getDefaultMessage());
            default:
                addLogMessage(exception, genericLogErrorMessage, logParameters);
                throw new AuthenticationServiceException(ErrorType.AUTH_GENERAL.getDefaultMessage(), exception);
        }
    }

    private void addLogMessage(Exception exception, String genericLogErrorMessage, Object... logParameters) {
        if (genericLogErrorMessage != null) {
            if (logParameters.length > 0) {
                log.error(genericLogErrorMessage, logParameters);
            } else {
                log.error(genericLogErrorMessage, exception);
            }
        }
    }
}
