/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


class BasicContentFilterTest {
    private final static String PRINCIPAL = "user";
    private final static char[] PASSWORD = "password".toCharArray();
    private final static String BASIC_AUTH = "Basic dXNlcjpwYXNzd29yZA==";

    private BasicContentFilter basicContentFilter;
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain filterChain = mock(FilterChain.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AuthenticationFailureHandler authenticationFailureHandler = mock(AuthenticationFailureHandler.class);
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler = mock(ResourceAccessExceptionHandler.class);

    @BeforeEach
    void setUp() {
        basicContentFilter = new BasicContentFilter(
            authenticationManager,
            authenticationFailureHandler,
            resourceAccessExceptionHandler);
    }

    @Test
    void authenticationWithBasicAuthHeader() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);

        AtomicBoolean called = new AtomicBoolean(false);
        doAnswer(invocation -> {
            assertArrayEquals(
                PASSWORD,
                (char[]) ((Authentication) invocation.getArguments()[0]).getCredentials()
            );
            assertEquals(PRINCIPAL, ((Authentication) invocation.getArguments()[0]).getPrincipal());
            called.set(true);
            return null;
        }).when(authenticationManager).authenticate(any());

        basicContentFilter.doFilter(request, response, filterChain);

        assertTrue(called.get());
        verify(filterChain).doFilter(request, response);
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    void shouldSkipFilter() throws ServletException, IOException {
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
    void shouldNotAuthenticateWithBadCredentials() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        AuthenticationException exception = new BadCredentialsException("Token not valid");

        when(authenticationManager.authenticate(any())).thenThrow(exception);

        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(any());
        verify(filterChain, never()).doFilter(any(), any());
        verify(authenticationFailureHandler).onAuthenticationFailure(request, response, exception);
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    void shouldNotAuthenticateWithNoGateway() throws ServletException, IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        RuntimeException exception = new RuntimeException("No Gateway");

        when(authenticationManager.authenticate(any())).thenThrow(exception);

        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(any());
        verify(filterChain, never()).doFilter(any(), any());
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler).handleException(request, response, exception);
    }

    @Test
    void shouldNotFilterWithNoCredentials() throws ServletException, IOException {
        basicContentFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(filterChain).doFilter(request, response);
        verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
    }

    @Test
    void extractContentFromRequestWithValidBasicAuth() {
        request.addHeader(HttpHeaders.AUTHORIZATION, BASIC_AUTH);
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertEquals("user", token.get().getPrincipal());
        assertArrayEquals(PASSWORD, (char[]) token.get().getCredentials());
    }

    @Test
    void extractContentFromRequestWithNonsenseBasicAuth() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlG4m3oFthR0n3syZA==");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertNull(token.get().getPrincipal());
        assertNull(token.get().getCredentials());
    }

    @Test
    void extractContentFromRequestWithNonsense() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Duck");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }

    @Test
    void extractContentFromRequestWithIncompleteBasicAuth() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcj11c2Vy");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertNull(token.get().getPrincipal());
        assertNull(token.get().getCredentials());
    }

    @Test
    void extractContentFromRequestWithEmptyRealm() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic ");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }

    @Test
    void extractContentFromRequestWithoutAuthHeader() {
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }
}

