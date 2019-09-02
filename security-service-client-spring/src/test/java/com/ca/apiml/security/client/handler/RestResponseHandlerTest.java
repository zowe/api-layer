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
import com.ca.mfaas.product.gateway.GatewayNotAvailableException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;


public class RestResponseHandlerTest {
    private final static String GENERIC_LOG_MESSAGE = "Generic Log Message";
    private final static String LOG_PARAMETERS = "https://localhost:8080/api/test/url";

    private HttpClientErrorException unauthorizedException;
    private HttpClientErrorException forbiddenException;
    private RestResponseHandler handler;

    @Before
    public void setUp() {
        handler = new RestResponseHandler();
        unauthorizedException = new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
        forbiddenException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
    }

    @Test(expected = BadCredentialsException.class)
    public void handleBadResponseWithBadCredentials() {
        handler.handleBadResponse(unauthorizedException, ErrorType.BAD_CREDENTIALS, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = TokenNotValidException.class)
    public void handleBadResponseWithTokenNotValid() {
        handler.handleBadResponse(unauthorizedException, ErrorType.TOKEN_NOT_VALID, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = TokenNotProvidedException.class)
    public void handleBadResponseWithTokenNotProvided() {
        handler.handleBadResponse(unauthorizedException, ErrorType.TOKEN_NOT_PROVIDED, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = BadCredentialsException.class)
    public void handleBadResponseWithAuthGeneral() {
        handler.handleBadResponse(unauthorizedException, ErrorType.AUTH_GENERAL, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    public void handleBadResponseWithCredentialsNotFound() {
        HttpClientErrorException badRequestException = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        handler.handleBadResponse(badRequestException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = AuthMethodNotSupportedException.class)
    public void handleBadResponseWithAuthMethodNotSupported() {
        HttpClientErrorException methodNotAllowedException = new HttpClientErrorException(HttpStatus.METHOD_NOT_ALLOWED);
        handler.handleBadResponse(methodNotAllowedException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = AuthenticationServiceException.class)
    public void handleBadResponseWithNoLogMessage() {
        handler.handleBadResponse(forbiddenException, null, GENERIC_LOG_MESSAGE);
    }

    @Test(expected = AuthenticationServiceException.class)
    public void handleBadResponseWithLogMessage() {
        handler.handleBadResponse(forbiddenException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = GatewayNotAvailableException.class)
    public void handleBadResponseWithGatewayNotAvailable() {
        ResourceAccessException raException = new ResourceAccessException("Resource Access Exception");
        handler.handleBadResponse(raException, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = GatewayNotAvailableException.class)
    public void handleBadResponseWithGatewayNotAvailableWithCause() {
        ResourceAccessException raException = new ResourceAccessException("Resource Access Exception");
        handler.handleBadResponse(raException, ErrorType.BAD_CREDENTIALS, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = ServiceNotAccessibleException.class)
    public void handleBadResponseWithServiceUnavailable() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication service unavailable");
        handler.handleBadResponse(exception, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }

    @Test(expected = HttpServerErrorException.class)
    public void handleBadResponseWithHttpServerError() {
        HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Some server error");
        handler.handleBadResponse(exception, null, GENERIC_LOG_MESSAGE, LOG_PARAMETERS);
    }
}
