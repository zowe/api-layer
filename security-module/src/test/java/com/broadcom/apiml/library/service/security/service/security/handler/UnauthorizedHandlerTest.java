/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.security.handler;

import com.broadcom.apiml.library.service.security.test.integration.error.ErrorService;
import com.broadcom.apiml.library.service.security.test.integration.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
public class UnauthorizedHandlerTest {
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
    public void unauthorizedHandlerTest() throws IOException {
        String requestUrl = "/api";
        String errorMessageKey = "com.ca.mfaas.security.authenticationRequired";

        AuthenticationException exception = mock(AuthenticationException.class);

        when(request.getRequestURI()).thenReturn(requestUrl);
        when(response.getWriter()).thenReturn(writer);
        when(errorService.createApiMessage(errorMessageKey, requestUrl)).thenReturn(apiMessage);

        UnauthorizedHandler unauthorizedHandler = new UnauthorizedHandler(errorService, mapper);
        unauthorizedHandler.commence(request, response, exception);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        verify(errorService).createApiMessage(errorMessageKey, requestUrl);
        verify(mapper).writeValue(writer, apiMessage);
    }
}
