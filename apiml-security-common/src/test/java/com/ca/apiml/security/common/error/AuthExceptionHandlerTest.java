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

import com.ca.apiml.security.common.token.TokenNotProvidedException;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.yaml.YamlMessageService;
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
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class AuthExceptionHandlerTest {

    @Autowired
    private MessageService messageService;

    @Mock
    private ObjectMapper objectMapper;

    private AuthExceptionHandler authExceptionHandler;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;


    @Before
    public void setup() {
        authExceptionHandler = new AuthExceptionHandler(messageService, objectMapper);
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        httpServletResponse = new MockHttpServletResponse();
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsInsufficientAuthenticationException() throws IOException, ServletException {
        authExceptionHandler.handleException(
            httpServletRequest,
            httpServletResponse,
            new InsufficientAuthenticationException("ERROR"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.AUTH_REQUIRED.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsBadCredentialsException() throws IOException, ServletException {
        authExceptionHandler.handleException(
            httpServletRequest,
            httpServletResponse,
            new BadCredentialsException("ERROR"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.BAD_CREDENTIALS.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsAuthenticationCredentialsNotFoundException() throws IOException, ServletException {
        authExceptionHandler.handleException(
            httpServletRequest,
            httpServletResponse,
            new AuthenticationCredentialsNotFoundException("ERROR"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsAuthMethodNotSupportedException() throws IOException, ServletException {
        AuthMethodNotSupportedException authMethodNotSupportedException = new AuthMethodNotSupportedException("ERROR");
        authExceptionHandler.handleException(httpServletRequest, httpServletResponse, authMethodNotSupportedException);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.AUTH_METHOD_NOT_SUPPORTED.getErrorMessageKey(), authMethodNotSupportedException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsTokenNotValidException() throws IOException, ServletException {
        authExceptionHandler.handleException(
            httpServletRequest,
            httpServletResponse,
            new TokenNotValidException("ERROR"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsTokenNotProvidedException() throws IOException, ServletException {
        authExceptionHandler.handleException(
            httpServletRequest,
            httpServletResponse,
            new TokenNotProvidedException("ERROR"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.TOKEN_NOT_PROVIDED.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testAuthenticationFailure_whenExceptionIsAuthenticationException() throws IOException, ServletException {
        AuthenticationServiceException serviceException = new AuthenticationServiceException("ERROR");
        authExceptionHandler.handleException(httpServletRequest, httpServletResponse, serviceException);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), httpServletResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());

        Message message = messageService.createMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), serviceException.getMessage(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Test
    public void testInvalidCertificateException() throws ServletException {
        authExceptionHandler.handleException(httpServletRequest, httpServletResponse, new InvalidCertificateException("method"));
        assertEquals(HttpStatus.FORBIDDEN.value(), httpServletResponse.getStatus());
    }

    @Test(expected = ServletException.class)
    public void testAuthenticationFailure_whenOccurUnexpectedException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest,
            httpServletResponse,
            new RuntimeException("unexpectedException"));
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/security-service-messages.yml");
        }
    }

}
