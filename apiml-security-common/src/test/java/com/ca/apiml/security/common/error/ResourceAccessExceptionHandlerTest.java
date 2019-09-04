/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.error;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAccessExceptionHandlerTest {

    private final ErrorService errorService = new ErrorServiceImpl("/security-service-messages.yml");

    private ResourceAccessExceptionHandler resourceAccessExceptionHandler;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;

    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        resourceAccessExceptionHandler = new ResourceAccessExceptionHandler(errorService, objectMapper);

        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        httpServletResponse = new MockHttpServletResponse();
    }

    @Test(expected = HttpServerErrorException.class)
    public void shouldRethrowException() throws ServletException {
        resourceAccessExceptionHandler.handleException(httpServletRequest, httpServletResponse, new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test(expected = ServletException.class)
    public void shouldThrowServletExceptionOnIOException() throws Exception {
        ApiMessage message = errorService.createApiMessage(ErrorType.GATEWAY_NOT_AVAILABLE.getErrorMessageKey(), httpServletRequest.getRequestURI());
        doThrow(new IOException("Error in writing response")).when(objectMapper).writeValue(httpServletResponse.getWriter(), message);

        resourceAccessExceptionHandler.writeErrorResponse(message, HttpStatus.NOT_FOUND, httpServletResponse);
    }
}
