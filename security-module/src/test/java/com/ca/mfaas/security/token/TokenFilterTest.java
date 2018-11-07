/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.token;

import com.ca.mfaas.security.config.SecurityConfigurationProperties;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TokenFilterTest {

    private SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private FilterChain filterChain = mock(FilterChain.class);
    private AuthenticationManager authenticationManager;
    private AuthenticationFailureHandler failureHandler;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        authenticationManager = mock(AuthenticationManager.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
    }

    @Test
    public void authenticationWithValidTokenInsideHeader() throws ServletException, IOException {
        String tokenValue = "token";
        String authorizationHeader = securityConfigurationProperties.getTokenProperties().getAuthorizationHeader();
        String bearerPrefix = securityConfigurationProperties.getTokenProperties().getBearerPrefix();
        String headerValue = bearerPrefix + tokenValue;
        TokenAuthentication authentication = new TokenAuthentication(tokenValue);

        when(request.getHeader(authorizationHeader)).thenReturn(headerValue);

        TokenFilter filter = new TokenFilter(authenticationManager, failureHandler, securityConfigurationProperties);
        filter.doFilter(request, response, filterChain);

        verify(request).getHeader(authorizationHeader);
        verify(authenticationManager).authenticate(authentication);
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void authenticationWithNotValidTokenInsideHeader() throws ServletException, IOException {
        String notValidToken = "token";
        String authorizationHeader = securityConfigurationProperties.getTokenProperties().getAuthorizationHeader();
        String headerValue = securityConfigurationProperties.getTokenProperties().getBearerPrefix() + notValidToken;
        TokenAuthentication authentication = new TokenAuthentication(notValidToken);
        BadCredentialsException exception = new BadCredentialsException("Bad token");

        when(request.getHeader(authorizationHeader)).thenReturn(headerValue);
        when(authenticationManager.authenticate(authentication)).thenThrow(exception);

        TokenFilter filter = new TokenFilter(authenticationManager, failureHandler, securityConfigurationProperties);
        filter.doFilter(request, response, filterChain);

        verify(request).getHeader(authorizationHeader);
        verify(failureHandler).onAuthenticationFailure(request, response, exception);
        verify(filterChain, never()).doFilter(any(), any());

    }

    @Test
    public void authenticationWithWrongHeader() throws ServletException, IOException {
        String headerValue = "Basic token";
        String authorizationHeader = securityConfigurationProperties.getTokenProperties().getAuthorizationHeader();
        when(request.getHeader(authorizationHeader)).thenReturn(headerValue);

        TokenFilter filter = new TokenFilter(authenticationManager, failureHandler, securityConfigurationProperties);
        filter.doFilter(request, response, filterChain);

        verify(request).getHeader(authorizationHeader);
        verify(authenticationManager, never()).authenticate(any());
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void callWithoutTokenInHeader() throws ServletException, IOException {
        String authorizationHeader = securityConfigurationProperties.getTokenProperties().getAuthorizationHeader();
        when(request.getHeader(authorizationHeader)).thenReturn(null);

        TokenFilter filter = new TokenFilter(authenticationManager, failureHandler, securityConfigurationProperties);
        filter.doFilter(request, response, filterChain);

        verify(request).getHeader(authorizationHeader);
        verify(authenticationManager, never()).authenticate(any());
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

}
