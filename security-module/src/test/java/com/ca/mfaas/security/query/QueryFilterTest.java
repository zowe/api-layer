/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.query;

import com.ca.mfaas.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.security.login.AuthMethodNotSupportedException;
import com.ca.mfaas.security.token.TokenAuthentication;
import com.ca.mfaas.security.token.TokenNotValidException;
import com.ca.mfaas.security.token.TokenService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QueryFilterTest {
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private SecurityConfigurationProperties securityConfigurationProperties;
    private QueryFilter queryFilter;
    private AuthenticationSuccessHandler authenticationSuccessHandler;
    private AuthenticationFailureHandler failureHandler;
    private AuthenticationManager authenticationManager;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        TokenService tokenService = new TokenService(securityConfigurationProperties);

        authenticationSuccessHandler = mock(AuthenticationSuccessHandler.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        authenticationManager = mock(AuthenticationManager.class);
        queryFilter = new QueryFilter(securityConfigurationProperties.getQueryPath(), authenticationSuccessHandler, failureHandler, tokenService, authenticationManager);
    }

    @Test
    public void attemptQueryWithWrongHttpMethod() {
        when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        exception.expect(AuthMethodNotSupportedException.class);
        exception.expectMessage("Authentication method not supported");

        queryFilter.attemptAuthentication(request, response);
    }

    @Test
    public void attemptAuthenticationWithCorrectCookie() {
        String username = "user";
        String token = "token";

        TokenAuthentication tokenAuthentication = new TokenAuthentication(username, token);
        tokenAuthentication.setAuthenticated(true);

        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), token);
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(tokenAuthentication);
        TokenAuthentication result = (TokenAuthentication) queryFilter.attemptAuthentication(request, response);

        assertThat(result, is(tokenAuthentication));
    }

    @Test
    public void attemptAuthenticationWithIncorrectCookie() {
        Cookie cookie = new Cookie("invalid_cookie", "token");
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        exception.expect(TokenNotValidException.class);
        exception.expectMessage("Valid token not provided.");

        queryFilter.attemptAuthentication(request, response);
    }

    @Test
    public void attemptAuthenticationWithNoCookie() {
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());

        exception.expect(TokenNotValidException.class);
        exception.expectMessage("Valid token not provided.");

        queryFilter.attemptAuthentication(request, response);
    }

    @Test
    public void attemptAuthenticationWithNullCookie() {
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), null);
        when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        exception.expect(TokenNotValidException.class);
        exception.expectMessage("Valid token not provided.");

        queryFilter.attemptAuthentication(request, response);
    }

    @Test
    public void successfulAuthentication() throws IOException, ServletException {
        FilterChain chain = mock(FilterChain.class);
        Authentication authentication = mock(Authentication.class);

        queryFilter.successfulAuthentication(request, response, chain, authentication);

        verify(authenticationSuccessHandler).onAuthenticationSuccess(request, response, authentication);
    }

    @Test
    public void unsuccessfulAuthentication() throws IOException, ServletException {
        AuthenticationException failed = mock(AuthenticationException.class);

        queryFilter.unsuccessfulAuthentication(request, response, failed);

        verify(failureHandler).onAuthenticationFailure(request, response, failed);
    }
}
