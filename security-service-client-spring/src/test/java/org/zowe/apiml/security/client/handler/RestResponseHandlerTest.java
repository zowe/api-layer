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

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.error.ZosAuthenticationException;
import org.zowe.apiml.security.common.token.InvalidTokenTypeException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestResponseHandlerTest {
    private final static String GENERIC_LOG_MESSAGE = "Generic Log Message";
    private final static String LOG_PARAMETERS = "https://localhost:8080/api/test/url";

    private HttpClientErrorException unauthorizedException;
    private HttpClientErrorException forbiddenException;
    private RestResponseHandler handler;
    private CloseableHttpResponse response;
    private StatusLine statusLine;

    @BeforeEach
    void setUp() {
        handler = new RestResponseHandler();
        unauthorizedException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        forbiddenException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        response = mock(CloseableHttpResponse.class);
        statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(401);
        when(response.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    void handleBadResponseWithBadCredentials() {

        assertThrows(BadCredentialsException.class, () -> {
            handler.handleErrorType(response, ErrorType.BAD_CREDENTIALS, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithTokenNotValid() {
        assertThrows(TokenNotValidException.class, () -> {
            handler.handleErrorType(response, ErrorType.TOKEN_NOT_VALID, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithTokenNotProvided() {
        assertThrows(TokenNotProvidedException.class, () -> {
            handler.handleErrorType(response, ErrorType.TOKEN_NOT_PROVIDED, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithTokenNotInResponse() {
        assertThrows(InvalidTokenTypeException.class,
            () -> handler.handleErrorType(response, ErrorType.INVALID_TOKEN_TYPE, GENERIC_LOG_MESSAGE, LOG_PARAMETERS));
    }

    @Test
    void handleBadResponseWithAuthGeneral() {
        assertThrows(BadCredentialsException.class, () -> {
            handler.handleErrorType(response, ErrorType.AUTH_GENERAL, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithCredentialsNotFound() {
        when(statusLine.getStatusCode()).thenReturn(400);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            handler.handleErrorType(response, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithAuthMethodNotSupported() {
        when(statusLine.getStatusCode()).thenReturn(405);
        assertThrows(AuthMethodNotSupportedException.class, () -> {
            handler.handleErrorType(response, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithNoLogMessage() {
        when(statusLine.getStatusCode()).thenReturn(504);
        assertThrows(AuthenticationServiceException.class, () -> {
            handler.handleErrorType(response, null, GENERIC_LOG_MESSAGE);
        });
    }

    @Test
    void handleBadResponseWithLogMessage() {
        when(statusLine.getStatusCode()).thenReturn(504);
        assertThrows(AuthenticationServiceException.class, () -> {
            handler.handleErrorType(response, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithGatewayNotAvailable() {
        IOException rootException = new IOException("Resource Access Exception");
        assertThrows(GatewayNotAvailableException.class, () -> {
            handler.handleException(rootException);
        });
    }

    @Test
    void handleBadResponseWithServiceUnavailable() {
        when(statusLine.getStatusCode()).thenReturn(503);
        assertThrows(ServiceNotAccessibleException.class, () -> {
            handler.handleErrorType(response, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleBadResponseWithHttpServerError() {
        when(statusLine.getStatusCode()).thenReturn(500);
        assertThrows(ServiceNotAccessibleException.class, () -> {
            handler.handleErrorType(response, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
    }

    @Test
    void handleUserSuspendedError() {
        ZosAuthenticationException exception = assertThrows(ZosAuthenticationException.class, () -> {
            handler.handleErrorType(response, ErrorType.USER_SUSPENDED, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
        assertEquals(163, exception.getPlatformError().errno);
    }

    @Test
    void handleInvalidNewPasswordError() {
        ZosAuthenticationException exception = assertThrows(ZosAuthenticationException.class, () -> {
            handler.handleErrorType(response, ErrorType.NEW_PASSWORD_INVALID, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
        assertEquals(169, exception.getPlatformError().errno);
    }

    @Test
    void handleExpiredPasswordError() {
        ZosAuthenticationException exception = assertThrows(ZosAuthenticationException.class, () -> {
            handler.handleErrorType(response, ErrorType.PASSWORD_EXPIRED, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
        });
        assertEquals(168, exception.getPlatformError().errno);
    }
}
