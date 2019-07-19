/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.query;

import com.ca.apiml.security.error.*;
import com.ca.apiml.security.token.TokenAuthentication;
import com.ca.apiml.security.token.TokenNotProvidedException;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class QueryFilterTest {
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private QueryFilter queryFilter;

    private final String VALID_TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJNZSIsImRvbSI6InRoaXMuY29tIiwibHRwYSI6Imx0cGFUb2tlbiIsImlhdCI6MTU1NDg4MzMzNCwiZXhwIjoxNTU0OTY5NzM0LCJpc3MiOiJBUElNTCIsImp0aSI6IjNkMzU3M2VhLWMxMzktNGE5Yy1iZDU5LWVjYmIyMmM0ZDcxZCJ9.bLe_d3b3bZC-K5K49fj1aHL_xDWMPsAgwKkrfewOrHhrxVL6lSphpGx52b8YvjaMUkFpVO12jCEDoYC1JLaQhQ";

    @Mock
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Mock
    private AuthenticationFailureHandler authenticationFailureHandler;

    @Mock
    private ResourceAccessExceptionHandler resourceAccessExceptionHandler;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthenticationService authenticationService;

    @Before
    public void setup() {
        queryFilter = new QueryFilter("TEST_ENDPOINT",
            authenticationSuccessHandler,
            authenticationFailureHandler,
            authenticationService,
            authenticationManager,
            resourceAccessExceptionHandler);
    }

    @Test
    public void shouldCallAuthenticationManagerAuthenticate() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.GET);
        httpServletResponse = new MockHttpServletResponse();
        queryFilter.setAuthenticationManager(authenticationManager);
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(
            Optional.of(VALID_TOKEN)
        );

        queryFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        verify(authenticationManager).authenticate(any());
    }

    @Test(expected = AuthMethodNotSupportedException.class)
    public void shouldRejectHttpMethods() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST);
        httpServletResponse = new MockHttpServletResponse();

        queryFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
    }

    @Test(expected = TokenNotProvidedException.class)
    public void shouldRejectIfTokenIsNotPresent() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.GET);
        httpServletResponse = new MockHttpServletResponse();
        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(
            Optional.empty()
        );

        queryFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
    }

    @Test
    public void shouldFailWithGatewayNotFound() throws IOException, ServletException {
        testFailWithResourceAccessError(new GatewayNotFoundException("API Gateway service not found"), ErrorType.GATEWAY_NOT_FOUND);
    }

    @Test
    public void shouldFailWithServiceNotAccessible() throws IOException, ServletException {
        testFailWithResourceAccessError(new ServiceNotAccessibleException("Authentication service not available"), ErrorType.SERVICE_UNAVAILABLE);
    }

    private void testFailWithResourceAccessError(RuntimeException exception, ErrorType errorType) throws IOException, ServletException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        ErrorService errorService = new ErrorServiceImpl("/security-service-messages.yml");
        ResourceAccessExceptionHandler resourceAccessExceptionHandler = new ResourceAccessExceptionHandler(errorService, objectMapper);
        queryFilter = new QueryFilter("TEST_ENDPOINT", authenticationSuccessHandler,
            authenticationFailureHandler, authenticationService, authenticationManager, resourceAccessExceptionHandler);

        when(authenticationService.getJwtTokenFromRequest(any())).thenReturn(Optional.of(VALID_TOKEN));
        when(authenticationManager.authenticate(new TokenAuthentication(VALID_TOKEN))).thenThrow(exception);

        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.GET);
        httpServletResponse = new MockHttpServletResponse();

        queryFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        ApiMessage message = errorService.createApiMessage(errorType.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }
}
