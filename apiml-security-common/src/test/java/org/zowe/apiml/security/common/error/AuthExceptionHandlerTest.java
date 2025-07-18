/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;
import org.zowe.apiml.security.common.token.InvalidTokenTypeException;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class AuthExceptionHandlerTest {

    @Autowired
    private MessageService messageService;

    @Mock
    private ObjectMapper objectMapper;

    private AuthExceptionHandler authExceptionHandler;
    private MockHttpServletRequest httpServletRequest;
    private final BiConsumer function = mock(BiConsumer.class);
    private final BiConsumer addHeader = mock(BiConsumer.class);

    @BeforeEach
    void setup() {
        authExceptionHandler = new AuthExceptionHandler(messageService, objectMapper, ApplicationInfo.builder().isModulith(false).build());
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

    }

    @Test
    void testAuthenticationFailure_whenExceptionIsInsufficientAuthenticationException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(), function, addHeader,
            new InsufficientAuthenticationException("ERROR"));

        Message message = messageService.createMessage(ErrorType.AUTH_REQUIRED.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsBadCredentialsException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new BadCredentialsException("ERROR"));

        Message message = messageService.createMessage(ErrorType.BAD_CREDENTIALS.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsAuthenticationCredentialsNotFoundException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new AuthenticationCredentialsNotFoundException("ERROR"));

        Message message = messageService.createMessage(ErrorType.AUTH_CREDENTIALS_NOT_FOUND.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsAuthMethodNotSupportedException() throws ServletException {
        AuthMethodNotSupportedException authMethodNotSupportedException = new AuthMethodNotSupportedException("ERROR");
        authExceptionHandler.handleException(httpServletRequest.getRequestURI(),
            function, addHeader, authMethodNotSupportedException);

        Message message = messageService.createMessage(ErrorType.METHOD_NOT_ALLOWED.getErrorMessageKey(), authMethodNotSupportedException.getMessage(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsTokenNotValidException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new TokenNotValidException("ERROR"));

        Message message = messageService.createMessage(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsNoMainframeIdException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new NoMainframeIdentityException("ERROR"));

        Message message = messageService.createMessage(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsTokenNotProvidedException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new TokenNotProvidedException("ERROR"));

        Message message = messageService.createMessage(ErrorType.TOKEN_NOT_PROVIDED.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure_whenExceptionIsTokenFormatNotValidException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new TokenFormatNotValidException("ERROR"));

        Message message = messageService.createMessage(ErrorType.TOKEN_NOT_VALID.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.BAD_REQUEST);
    }


    @Test
    void testAuthenticationFailure_whenExceptionIsAuthenticationException() throws ServletException {
        AuthenticationServiceException serviceException = new AuthenticationServiceException("ERROR");
        authExceptionHandler.handleException(httpServletRequest.getRequestURI(),
            function, addHeader, serviceException);

        Message message = messageService.createMessage(ErrorType.AUTH_GENERAL.getErrorMessageKey(), serviceException.getMessage(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testInvalidCertificateException() throws ServletException {
        authExceptionHandler.handleException(httpServletRequest.getRequestURI(),
            function, addHeader, new InvalidCertificateException("method"));

        verify(function).accept(any(), eq(HttpStatus.FORBIDDEN));
    }

    @Test
    void testZosAuthenticationExceptionException() throws ServletException {
        PlatformReturned platformReturned = PlatformReturned.builder().success(false).errno(PlatformPwdErrno.EACCES.errno).build();
        authExceptionHandler.handleException(httpServletRequest.getRequestURI(),
            function, addHeader, new ZosAuthenticationException(platformReturned));

        verify(function).accept(any(), eq(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void testTokenNotInResponseException() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new InvalidTokenTypeException("ERROR"));

        Message message = messageService.createMessage(ErrorType.INVALID_TOKEN_TYPE.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void testAuthenticationFailure_whenOccurUnexpectedException() {
        assertThrows(ServletException.class, () -> {
            authExceptionHandler.handleException(
                httpServletRequest.getRequestURI(),
                function, addHeader,
                new RuntimeException("unexpectedException"));
        });
    }

    @Test
    void testAuthServiceUnavailable() throws ServletException {
        authExceptionHandler.handleException(
            httpServletRequest.getRequestURI(),
            function, addHeader,
            new ServiceNotAccessibleException("URI"));

        Message message = messageService.createMessage(ErrorType.SERVICE_UNAVAILABLE.getErrorMessageKey(), httpServletRequest.getRequestURI());

        verify(function).accept(message.mapToView(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @TestConfiguration
    static class ContextConfiguration {

        @Bean
        MessageService messageService() {
            return new YamlMessageService("/security-service-messages.yml");
        }

    }

}
