/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.login;

import com.ca.apiml.security.error.AuthMethodNotSupportedException;
import com.ca.apiml.security.error.ResourceAccessExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.ws.rs.HttpMethod;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LoginFilterTest {
    private static final String VALID_JSON = "{\"username\": \"user\", \"password\": \"pwd\"}";
    private static final String EMPTY_JSON = "{\"username\": \"\", \"password\": \"\"}";
    private static final String VALID_AUTH_HEADER = "Basic dXNlcjpwd2Q=";
    private static final String INVALID_AUTH_HEADER = "Basic dXNlcj11c2Vy";

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

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        loginFilter = new LoginFilter("TEST_ENDPOINT",
            authenticationSuccessHandler,
            authenticationFailureHandler,
            mapper,
            authenticationManager,
            resourceAccessExceptionHandler);
    }

    @Test
    public void shouldCallAuthenticationManagerAuthenticateWithAuthHeader() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST);
        httpServletRequest.addHeader(HttpHeaders.AUTHORIZATION, VALID_AUTH_HEADER);
        httpServletResponse = new MockHttpServletResponse();

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        UsernamePasswordAuthenticationToken authentication
            = new UsernamePasswordAuthenticationToken("user", "pwd");
        verify(authenticationManager).authenticate(authentication);
    }

    @Test
    public void shouldCallAuthenticationManagerAuthenticateWithJson() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST);
        httpServletRequest.setContent(VALID_JSON.getBytes());
        httpServletResponse = new MockHttpServletResponse();

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);

        UsernamePasswordAuthenticationToken authentication
            = new UsernamePasswordAuthenticationToken("user", "pwd");
        verify(authenticationManager).authenticate(authentication);
    }

    @Test
    public void shouldFailWithJsonEmptyCredentials() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST);
        httpServletRequest.setContent(EMPTY_JSON.getBytes());
        httpServletResponse = new MockHttpServletResponse();

        exception.expect(AuthenticationCredentialsNotFoundException.class);
        exception.expectMessage("Username or password not provided.");

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
    }

    @Test
    public void shouldFailWithoutAuth() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST);
        httpServletResponse = new MockHttpServletResponse();

        exception.expect(AuthenticationCredentialsNotFoundException.class);
        exception.expectMessage("Login object has wrong format.");

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
    }

    @Test
    public void shouldFailWithWrongHttpMethod() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.GET);
        httpServletResponse = new MockHttpServletResponse();

        exception.expect(AuthMethodNotSupportedException.class);
        exception.expectMessage("GET");

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
    }

    @Test
    public void shouldFailWithIncorrectCredentialsFormat() throws ServletException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST);
        httpServletRequest.addHeader(HttpHeaders.AUTHORIZATION, INVALID_AUTH_HEADER);
        httpServletResponse = new MockHttpServletResponse();

        exception.expect(AuthenticationCredentialsNotFoundException.class);
        exception.expectMessage("Login object has wrong format.");

        loginFilter.attemptAuthentication(httpServletRequest, httpServletResponse);
    }
}
