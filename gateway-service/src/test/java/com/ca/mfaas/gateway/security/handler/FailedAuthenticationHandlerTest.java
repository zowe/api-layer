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
import com.ca.mfaas.gateway.security.login.InvalidUserException;
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

@SuppressWarnings("Duplicates")
public class FailedAuthenticationHandlerTest {
    private ObjectMapper mapper;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private PrintWriter writer;
    private ErrorService errorService;
    private ApiMessage apiMessage;

    @Before
    public void setUp() {
        mapper = mock(ObjectMapper.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = mock(PrintWriter.class);
        errorService = mock(ErrorService.class);
        apiMessage = mock(ApiMessage.class);
    }

    @Test
    public void generalFailedAuthentication() throws IOException {
        String requestUrl = "/api";
        String exceptionMessage = "auth fail";
        String errorMessageKey = "com.ca.mfaas.security.authenticationException";
        AuthenticationException exception = mock(AuthenticationException.class);

        when(request.getRequestURI()).thenReturn(requestUrl);
        when(response.getWriter()).thenReturn(writer);
        when(exception.getMessage()).thenReturn(exceptionMessage);
        when(errorService.createApiMessage(errorMessageKey, exceptionMessage, requestUrl)).thenReturn(apiMessage);

        FailedAuthenticationHandler failedAuthenticationHandler = new FailedAuthenticationHandler(errorService, mapper);
        failedAuthenticationHandler.onAuthenticationFailure(request, response, exception);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        verify(errorService).createApiMessage(errorMessageKey, exceptionMessage, requestUrl);
        verify(mapper).writeValue(writer, apiMessage);
    }

    @Test
    public void invalidUser() throws IOException {
        String requestUrl = "/api";
        String exceptionMessage = "auth fail";
        String errorMessageKey = "com.ca.mfaas.security.invalidUsername";
        AuthenticationException exception = mock(InvalidUserException.class);

        when(request.getRequestURI()).thenReturn(requestUrl);
        when(response.getWriter()).thenReturn(writer);
        when(exception.getMessage()).thenReturn(exceptionMessage);
        when(errorService.createApiMessage(errorMessageKey, exceptionMessage, requestUrl)).thenReturn(apiMessage);

        FailedAuthenticationHandler failedAuthenticationHandler = new FailedAuthenticationHandler(errorService, mapper);
        failedAuthenticationHandler.onAuthenticationFailure(request, response, exception);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        verify(errorService).createApiMessage(errorMessageKey, exceptionMessage, requestUrl);
        verify(mapper).writeValue(writer, apiMessage);
    }

    @Test
    public void expiredToken() throws IOException {
        String requestUrl = "/api";
        String exceptionMessage = "auth fail";
        String errorMessageKey = "com.ca.mfaas.security.tokenIsExpired";
        AuthenticationException exception = mock(TokenExpireException.class);

        when(request.getRequestURI()).thenReturn(requestUrl);
        when(response.getWriter()).thenReturn(writer);
        when(exception.getMessage()).thenReturn(exceptionMessage);
        when(errorService.createApiMessage(errorMessageKey, exceptionMessage, requestUrl)).thenReturn(apiMessage);

        FailedAuthenticationHandler failedAuthenticationHandler = new FailedAuthenticationHandler(errorService, mapper);
        failedAuthenticationHandler.onAuthenticationFailure(request, response, exception);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        verify(errorService).createApiMessage(errorMessageKey, exceptionMessage, requestUrl);
        verify(mapper).writeValue(writer, apiMessage);
    }

    @Test
    public void notValidToken() throws IOException {
        String requestUrl = "/api";
        String exceptionMessage = "auth fail";
        String errorMessageKey = "com.ca.mfaas.security.tokenIsNotValid";
        AuthenticationException exception = mock(TokenNotValidException.class);

        when(request.getRequestURI()).thenReturn(requestUrl);
        when(response.getWriter()).thenReturn(writer);
        when(exception.getMessage()).thenReturn(exceptionMessage);
        when(errorService.createApiMessage(errorMessageKey, exceptionMessage, requestUrl)).thenReturn(apiMessage);

        FailedAuthenticationHandler failedAuthenticationHandler = new FailedAuthenticationHandler(errorService, mapper);
        failedAuthenticationHandler.onAuthenticationFailure(request, response, exception);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        verify(errorService).createApiMessage(errorMessageKey, exceptionMessage, requestUrl);
        verify(mapper).writeValue(writer, apiMessage);
    }

}
