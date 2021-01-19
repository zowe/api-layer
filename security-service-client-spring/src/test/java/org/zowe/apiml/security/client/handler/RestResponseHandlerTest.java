/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.client.handler;

import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.junit.jupiter.api.Assertions.assertThrows;


class RestResponseHandlerTest {
    private final static String GENERIC_LOG_MESSAGE = "Generic Log Message";
    private final static String LOG_PARAMETERS = "https://localhost:8080/api/test/url";

    private HttpClientErrorException unauthorizedException;
    private HttpClientErrorException forbiddenException;
    private RestResponseHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RestResponseHandler();
        unauthorizedException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        forbiddenException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleBadResponseWithBadCredentials() {
        assertThrows(BadCredentialsException.class, () -> {
            handler.handleBadResponse(unauthorizedException, ErrorType.BAD_CREDENTIALS, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithTokenNotValid() {
        assertThrows(TokenNotValidException.class, () -> {
            handler.handleBadResponse(unauthorizedException, ErrorType.TOKEN_NOT_VALID, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithTokenNotProvided() {
        assertThrows(TokenNotProvidedException.class, () -> {
            handler.handleBadResponse(unauthorizedException, ErrorType.TOKEN_NOT_PROVIDED, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithAuthGeneral() {
        assertThrows(BadCredentialsException.class, () -> {
            handler.handleBadResponse(unauthorizedException, ErrorType.AUTH_GENERAL, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithCredentialsNotFound() {
        HttpClientErrorException badRequestException = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            handler.handleBadResponse(badRequestException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithAuthMethodNotSupported() {
        HttpClientErrorException methodNotAllowedException = new HttpClientErrorException(HttpStatus.METHOD_NOT_ALLOWED);
        assertThrows(AuthMethodNotSupportedException.class, () -> {
            handler.handleBadResponse(methodNotAllowedException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithNoLogMessage() {
        assertThrows(AuthenticationServiceException.class, () -> {
            handler.handleBadResponse(forbiddenException, null, GENERIC_LOG_MESSAGE);
        });
    }

    @Test
    void handleBadResponseWithLogMessage() {
        assertThrows(AuthenticationServiceException.class, () -> {
            handler.handleBadResponse(forbiddenException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithGatewayNotAvailable() {
        ResourceAccessException raException = new ResourceAccessException("Resource Access Exception");
        assertThrows(GatewayNotAvailableException.class, () -> {
            handler.handleBadResponse(raException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithGatewayNotAvailableWithCause() {
        ResourceAccessException raException = new ResourceAccessException("Resource Access Exception");
        assertThrows(GatewayNotAvailableException.class, () -> {
            handler.handleBadResponse(raException, ErrorType.BAD_CREDENTIALS, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithServiceUnavailable() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable");
        assertThrows(ServiceNotAccessibleException.class, () -> {
            handler.handleBadResponse(exception, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithHttpServerError() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Some server error");
        assertThrows(HttpServerErrorException.class, () -> {
            handler.handleBadResponse(exception, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }
}
