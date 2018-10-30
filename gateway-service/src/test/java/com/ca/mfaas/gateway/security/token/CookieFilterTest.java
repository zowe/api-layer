/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.token;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CookieFilterTest {
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private FilterChain filterChain = mock(FilterChain.class);
    private MFaaSConfigPropertiesContainer propertiesContainer = new MFaaSConfigPropertiesContainer();
    private AuthenticationManager authenticationManager;
    private AuthenticationFailureHandler failureHandler;

    @Before
    public void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        propertiesContainer.setSecurity(new MFaaSConfigPropertiesContainer.SecurityProperties());
    }

    @Test
    public void authenticationWithValidTokenInsideCookie() throws ServletException, IOException {
        String token = "token";
        TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
        Cookie cookie = new Cookie(propertiesContainer.getSecurity().getCookieProperties().getCookieName(), token);
        Cookie[] cookies = new Cookie[]{ cookie };

        when(request.getCookies()).thenReturn(cookies);

        CookieFilter cookieFilter = new CookieFilter(authenticationManager, failureHandler, propertiesContainer);
        cookieFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(tokenAuthentication);
        verify(filterChain).doFilter(request, response);
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    public void authenticationWithNotValidTokenInsideCookie() throws ServletException, IOException {
        String notValidToken = "token";
        TokenAuthentication tokenAuthentication = new TokenAuthentication(notValidToken);
        Cookie cookie = new Cookie(propertiesContainer.getSecurity().getCookieProperties().getCookieName(), notValidToken);
        Cookie[] cookies = new Cookie[]{ cookie };
        BadCredentialsException exception = new BadCredentialsException("Bad token");

        when(request.getCookies()).thenReturn(cookies);
        when(authenticationManager.authenticate(tokenAuthentication)).thenThrow(exception);

        CookieFilter cookieFilter = new CookieFilter(authenticationManager, failureHandler, propertiesContainer);
        cookieFilter.doFilter(request, response, filterChain);

        verify(authenticationManager).authenticate(tokenAuthentication);
        verify(filterChain, never()).doFilter(any(), any());
        verify(failureHandler).onAuthenticationFailure(request, response, exception);
    }

    @Test
    public void authenticationWithoutCookies() throws ServletException, IOException {
        when(request.getCookies()).thenReturn(null);

        CookieFilter cookieFilter = new CookieFilter(authenticationManager, failureHandler, propertiesContainer);
        cookieFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void authenticationWithWrongCookie() throws ServletException, IOException {
        String token = "token";
        Cookie cookie = new Cookie("someCookie", token);
        Cookie[] cookies = new Cookie[]{ cookie };

        when(request.getCookies()).thenReturn(cookies);

        CookieFilter cookieFilter = new CookieFilter(authenticationManager, failureHandler, propertiesContainer);
        cookieFilter.doFilter(request, response, filterChain);

        verify(authenticationManager, never()).authenticate(any());
        verify(failureHandler, never()).onAuthenticationFailure(any(), any(), any());
        verify(filterChain).doFilter(request, response);
    }

}
