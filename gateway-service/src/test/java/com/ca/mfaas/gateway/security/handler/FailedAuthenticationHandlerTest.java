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
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.gateway.security.AuthMethodNotSupportedException;
import com.ca.mfaas.gateway.security.query.TokenNotProvidedException;
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;


@RunWith(SpringRunner.class)
public class FailedAuthenticationHandlerTest {

    @Autowired
    private ErrorService errorService;

    @Mock
    private ObjectMapper objectMapper;

    private FailedAuthenticationHandler failedAuthenticationHandler;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;


    @Before
    public void setup() {
        failedAuthenticationHandler = new FailedAuthenticationHandler(errorService, objectMapper);
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        httpServletResponse = new MockHttpServletResponse();
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsBadCredentialsException() throws IOException {
        BadCredentialsException badCredentialsException = new BadCredentialsException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, badCredentialsException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.login.invalidCredentials", httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsAuthenticationCredentialsNotFoundException() throws IOException {
        AuthenticationCredentialsNotFoundException credentialsException = new AuthenticationCredentialsNotFoundException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, credentialsException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.login.invalidInput", httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsAuthMethodNotSupportedException() throws IOException {
        AuthMethodNotSupportedException authMethodNotSupportedException = new AuthMethodNotSupportedException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, authMethodNotSupportedException);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.invalidMethod", authMethodNotSupportedException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsTokenNotValidException() throws IOException {
        TokenNotValidException tokenNotValidException = new TokenNotValidException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, tokenNotValidException);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.query.invalidToken", httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsTokenNotProvidedException() throws IOException {
        TokenNotProvidedException tokenNotProvidedException = new TokenNotProvidedException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, tokenNotProvidedException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.query.tokenNotProvided", httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Test
    public void testAuthenticationFailure_whenOccurUnexpectedException() throws IOException {
        TokenExpireException tokenExpireException = new TokenExpireException("ERROR");
        failedAuthenticationHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, tokenExpireException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        ApiMessage message = errorService.createApiMessage("apiml.gateway.security.generic", tokenExpireException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }


    @Configuration
    static class ContextConfiguration {
        @Bean
        public ErrorService errorService() {
            return new ErrorServiceImpl("/gateway-messages.yml");
        }
    }

}
