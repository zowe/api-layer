/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.content;

import com.ca.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BasicContentFilterTest {
    private final static String PRINCIPAL = "user";
    private final static String PASSWORD = "password";
    private final static String BASIC_AUTH = "Basic dXNlcjpwYXNzd29yZA==";

    private BasicContentFilter basicContentFilter;
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain filterChain = mock(FilterChain.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AuthenticationFailureHandler authenticationFailureHandler = mock(AuthenticationFailureHandler.class);
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler = mock(ResourceAccessExceptionHandler.class);

    @Before
    public void setUp() {
        basicContentFilter = new BasicContentFilter(
            authenticationManager,
            authenticationFailureHandler,
            resourceAccessExceptionHandler);
    }

    @Test
    public void authenticationWithBasicAuthHeader() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, PASSWORD);

        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(authentication);
        verify(filterChain).doFilter(request, response);
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void shouldSkipFilter() throws ServletException, IOException {
        String[] endpoints = {"/gateway"};

        request.setContextPath(endpoints[0]);

        BasicContentFilter basicContentFilter = new BasicContentFilter(authenticationManager,
            authenticationFailureHandler,
            resourceAccessExceptionHandler,
            endpoints);

        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }


    @Test
    public void shouldNotAuthenticateWithBadCredentials() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, PASSWORD);
        AuthenticationException exception = new BadCredentialsException("Token not valid");

        when(authenticationManager.authenticate(authentication)).thenThrow(exception);

        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(authentication);
        verify(filterChain, never()).doFilter(any(), any());
        verify(authenticationFailureHandler).onAuthenticationFailure(request, response, exception);
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void shouldNotAuthenticateWithNoGateway() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(PRINCIPAL, PASSWORD);
        RuntimeException exception = new RuntimeException("No Gateway");

        when(authenticationManager.authenticate(authentication)).thenThrow(exception);

        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(authentication);
        verify(filterChain, never()).doFilter(any(), any());
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler).handleException(request, response, exception);
    }

    @Test
    public void shouldNotFilterWithNoCookie() throws ServletException, IOException {
        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(filterChain).doFilter(request, response);
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    public void extractContentFromRequestWithValidBasicAuth() {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertEquals("user", token.get().getPrincipal());
        assertEquals("password", token.get().getCredentials().toString());
    }

    @Test
    public void extractContentFromRequestWithNonsenseBasicAuth() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlG4m3oFthR0n3syZA==");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertNull(token.get().getPrincipal());
        assertNull(token.get().getCredentials());
    }

    @Test
    public void extractContentFromRequestWithNonsense() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Duck");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }

    @Test
    public void extractContentFromRequestWithIncompleteBasicAuth() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcj11c2Vy");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertNull(token.get().getPrincipal());
        assertNull(token.get().getCredentials());
    }

    @Test
    public void extractContentFromRequestWithEmptyRealm() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic ");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }

    @Test
    public void extractContentFromRequestWithoutAuthHeader() {
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }
}

