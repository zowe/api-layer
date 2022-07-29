/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;
import org.zowe.apiml.security.common.error.*;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class LoginFilterTest {
    private static final String VALID_JSON = "{\"username\": \"user\", \"password\": \"pwd\"}";
    private static final String JSON_WITH_NEW_PW = "{\"username\": \"user\", \"password\": \"pwd\", \"newPassword\": \"newPwd\"}";
    private static final String EMPTY_JSON = "{\"username\": \"\", \"password\": \"\"}";
    private static final String VALID_AUTH_HEADER = "Basic dXNlcjpwd2Q=";
    private static final String INVALID_AUTH_HEADER = "Basic dXNlcj11c2Vy";
    private static final String INVALID_ENCODED_AUTH_HEADER = "Basic dXNlcj1";
    private static final String USER = "user";
    private static final String PASSWORD = "pwd";
    private static final String NEW_PASSWORD = "newPwd";

    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private LoginFilter loginFilter;
    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @Mock
    private AuthenticationFailureHandler authenticationFailureHandler;

    @Mock
    private ResourceAccessExceptionHandler resourceAccessExceptionHandler;

    @Mock
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setup() {
        loginFilter = new LoginFilter("TEST_ENDPOINT",
            authenticationSuccessHandler,
            authenticationFailureHandler,
            mapper,
            authenticationManager,
            resourceAccessExceptionHandler);
    }

    @Test
    void shouldCallAuthenticationManagerAuthenticateWithAuthHeader() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.addHeader(HttpHeaders.AUTHORIZATION, VALID_AUTH_HEADER);
        httpServletResponse = new MockHttpServletResponse();

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        UsernamePasswordAuthenticationToken authentication
            = new UsernamePasswordAuthenticationToken(USER, new LoginRequest(USER,PASSWORD));
        verify(authenticationManager).authenticate(authentication);
    }

    @Test
    void shouldCallAuthenticationManagerAuthenticateWithJson() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.setContent(VALID_JSON.getBytes());
        httpServletResponse = new MockHttpServletResponse();

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        UsernamePasswordAuthenticationToken authentication
            = new UsernamePasswordAuthenticationToken(USER, new LoginRequest(USER,PASSWORD));
        verify(authenticationManager).authenticate(authentication);
    }

    @Test
    void shouldCallAuthenticationManagerAuthenticateWithNewPasswordInJson() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.setContent(JSON_WITH_NEW_PW.getBytes());
        httpServletResponse = new MockHttpServletResponse();

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        UsernamePasswordAuthenticationToken authentication
            = new UsernamePasswordAuthenticationToken(USER, new LoginRequest(USER,PASSWORD, NEW_PASSWORD));
        verify(authenticationManager).authenticate(authentication);
    }

    @Test
    void shouldFailWithJsonEmptyCredentials() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.setContent(EMPTY_JSON.getBytes());
        httpServletResponse = new MockHttpServletResponse();

        Exception exception = assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
        });
        assertEquals("Username or password not provided.", exception.getMessage());
    }

    @Test
    void shouldReturnNullWithoutAuth() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletResponse = new MockHttpServletResponse();

        assertThat(loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse), is(nullValue()));
    }

    @Test
    void shouldFailWithWrongHttpMethod() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.GET.name());
        httpServletResponse = new MockHttpServletResponse();

        Exception exception = assertThrows(AuthMethodNotSupportedException.class, () -> {
            loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
        });
        assertEquals("GET", exception.getMessage());
    }

    @Test
    void shouldFailWithIncorrectCredentialsFormat() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.addHeader(HttpHeaders.AUTHORIZATION, INVALID_AUTH_HEADER);
        httpServletResponse = new MockHttpServletResponse();

        Exception exception = assertThrows(BadCredentialsException.class, () -> {
            loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
        });
        assertEquals("Invalid basic authentication header", exception.getMessage());
    }

    @Test
    void shouldFailWithIncorrectlyEncodedBase64() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.addHeader(HttpHeaders.AUTHORIZATION, INVALID_ENCODED_AUTH_HEADER);
        httpServletResponse = new MockHttpServletResponse();

        Exception exception = assertThrows(BadCredentialsException.class, () -> {
            loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
        });
        assertEquals("Invalid basic authentication header", exception.getMessage());
    }

    @Test
    void shouldFailWithGatewayNotAvailable() throws IOException, ServletException {
        testFailWithResourceAccessError(new GatewayNotAvailableException("API Gateway service not available"), ErrorType.GATEWAY_NOT_AVAILABLE);
    }

    @Test
    void shouldFailWithServiceNotAccessible() throws IOException, ServletException {
        testFailWithResourceAccessError(new ServiceNotAccessibleException("Authentication service not available"), ErrorType.SERVICE_UNAVAILABLE);
    }

    private void testFailWithResourceAccessError(RuntimeException exception, ErrorType errorType) throws IOException, ServletException {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        MessageService messageService = new YamlMessageService("/security-service-messages.yml");
        ResourceAccessExceptionHandler resourceAccessExceptionHandler = new ResourceAccessExceptionHandler(messageService, objectMapper);
        loginFilter = new LoginFilter("TEST_ENDPOINT", authenticationSuccessHandler,
            authenticationFailureHandler, objectMapper, authenticationManager,
            resourceAccessExceptionHandler);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(USER, new LoginRequest(USER,PASSWORD));
        when(authenticationManager.authenticate(authentication)).thenThrow(exception);

        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.addHeader(HttpHeaders.AUTHORIZATION, VALID_AUTH_HEADER);
        httpServletResponse = new MockHttpServletResponse();

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        Message message = messageService.createMessage(errorType.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }
}
