/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.security.login;

import com.broadcom.apiml.service.security.token.TokenAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@Ignore
public class LoginFilterTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private LoginFilter loginFilter;
    private String authEndpoint;
    private AuthenticationSuccessHandler successHandler;
    private AuthenticationFailureHandler failureHandler;
    private ObjectMapper mapper;
    private AuthenticationManager authenticationManager;
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);

    @Before
    public void setUp() {
        authEndpoint = "/auth/login";
        successHandler = mock(AuthenticationSuccessHandler.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        mapper = mock(ObjectMapper.class);
        authenticationManager = mock(AuthenticationManager.class);
        loginFilter = new LoginFilter(authEndpoint, successHandler, failureHandler, mapper, authenticationManager);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

    }

    @Test
    public void attemptAuthenticationWithWrongHttpMethodTest() {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        exception.expect(AuthMethodNotSupportedException.class);
        exception.expectMessage("Authentication method not supported");

        loginFilter.attemptAuthentication(request, response);
    }

    @Test
    public void attemptAuthenticationWithWringLoginObjectFormat() throws IOException {
        ServletInputStream inputStream = mock(ServletInputStream.class);

        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(request.getInputStream()).thenReturn(inputStream);
        when(mapper.readValue(any(InputStream.class), isA(Class.class))).thenThrow(new IOException());
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Login object has wrong format");

        loginFilter.attemptAuthentication(request, response);
    }

    @Test
    public void attemptAuthenticationWithCorrectLoginObject() throws IOException {
        String username = "user";
        String password = "password";
        String token = "token";
        LoginRequest loginRequest = new LoginRequest(username, password);
        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token);
        tokenAuthentication.setAuthenticated(true);
        ServletInputStream inputStream = mock(ServletInputStream.class);

        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(mapper.readValue(any(InputStream.class), isA(Class.class))).thenReturn(loginRequest);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(tokenAuthentication);

        TokenAuthentication result = (TokenAuthentication) loginFilter.attemptAuthentication(request, response);

        assertThat(result, is(tokenAuthentication));
    }

    @Test
    public void attemptAuthenticationWithBlankFieldsOfLoginObject() throws IOException {
        String username = "";
        String password = "";
        String token = "token";
        LoginRequest loginRequest = new LoginRequest(username, password);
        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token);
        tokenAuthentication.setAuthenticated(true);
        ServletInputStream inputStream = mock(ServletInputStream.class);

        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(mapper.readValue(any(InputStream.class), isA(Class.class))).thenReturn(loginRequest);
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password not provided");

        loginFilter.attemptAuthentication(request, response);
    }

    @Test
    public void attemptAuthenticationWithBlankPasswordFiledOfLoginObject() throws IOException {
        String username = "user";
        String password = "";
        String token = "token";
        LoginRequest loginRequest = new LoginRequest(username, password);
        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token);
        tokenAuthentication.setAuthenticated(true);
        ServletInputStream inputStream = mock(ServletInputStream.class);

        when(request.getInputStream()).thenReturn(inputStream);
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        when(mapper.readValue(any(InputStream.class), isA(Class.class))).thenReturn(loginRequest);
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Username or password not provided");

        loginFilter.attemptAuthentication(request, response);
    }

    @Test
    public void successfulAuthenticationTest() throws IOException, ServletException {
        FilterChain chain = mock(FilterChain.class);
        Authentication authentication = mock(Authentication.class);

        loginFilter.successfulAuthentication(request, response, chain, authentication);

        verify(successHandler).onAuthenticationSuccess(request, response, authentication);
    }

    @Test
    public void unsuccessfulAuthenticationTest() throws IOException, ServletException {
        AuthenticationException failed = mock(AuthenticationException.class);

        loginFilter.unsuccessfulAuthentication(request, response, failed);

        verify(failureHandler).onAuthenticationFailure(request, response, failed);
    }
}
