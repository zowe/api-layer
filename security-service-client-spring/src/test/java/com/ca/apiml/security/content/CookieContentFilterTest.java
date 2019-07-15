/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.content;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.error.ResourceAccessExceptionHandler;
import com.ca.apiml.security.token.TokenAuthentication;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CookieContentFilterTest {

    private CookieContentFilter cookieContentFilter;
    private final SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain filterChain = mock(FilterChain.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AuthenticationFailureHandler failureHandler = mock(AuthenticationFailureHandler.class);
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler = mock(ResourceAccessExceptionHandler.class);

    @Before
    public void setUp() {
        cookieContentFilter = new CookieContentFilter(authenticationManager,
            failureHandler,
            resourceAccessExceptionHandler,
            securityConfigurationProperties);
    }

    @Test
    public void authenticationWithValidTokenInsideCookie() throws ServletException, IOException {
        String token = "token";

        TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), token);
        request.setCookies(cookie);

        cookieContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(tokenAuthentication);
        verify(filterChain).doFilter(request, response);
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void shouldSkipFilter() throws ServletException, IOException {
        String[] endpoints = {"/gateway"};

        request.setContextPath(endpoints[0]);

        CookieContentFilter cookieContentFilter = new CookieContentFilter(authenticationManager,
            failureHandler,
            resourceAccessExceptionHandler,
            securityConfigurationProperties,
            endpoints);

        cookieContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void shouldNotAuthenticateWithBadCredentials() throws ServletException, IOException {
        String token = "token";
        AuthenticationException exception = new BadCredentialsException("Token not valid");

        TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), token);
        request.setCookies(cookie);

        when(authenticationManager.authenticate(tokenAuthentication)).thenThrow(exception);

        cookieContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(tokenAuthentication);
        verify(filterChain, never()).doFilter(any(), any());
        verify(failureHandler).onAuthenticationFailure(request, response, exception);
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void shouldNotAuthenticateWithNoGateway() throws ServletException, IOException {
        String token = "token";
        RuntimeException exception = new RuntimeException("No Gateway");

        TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), token);
        request.setCookies(cookie);

        when(authenticationManager.authenticate(tokenAuthentication)).thenThrow(exception);

        cookieContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(tokenAuthentication);
        verify(filterChain, never()).doFilter(any(), any());
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler).handleException(request, response, exception);
    }

    @Test
    public void shouldNotFilterWithNoCookie() throws ServletException, IOException {
        cookieContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(filterChain).doFilter(request, response);
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void shouldReturnEmptyIfNoCookies() {
        Optional<AbstractAuthenticationToken> content = cookieContentFilter.extractContent(request);

        assertEquals(Optional.empty(), content);
    }

    @Test
    public void shouldExtractContent() {
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "cookie");
        request.setCookies(cookie);

        Optional<AbstractAuthenticationToken> content = cookieContentFilter.extractContent(request);

        TokenAuthentication actualToken = new TokenAuthentication(cookie.getValue());

        assertTrue(content.isPresent());
        assertEquals(actualToken, content.get());

    }

    @Test
    public void shouldReturnEmptyIfCookieValueIsEmpty() {
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "");
        request.setCookies(cookie);

        Optional<AbstractAuthenticationToken> content = cookieContentFilter.extractContent(request);

        assertFalse(content.isPresent());
    }
}
