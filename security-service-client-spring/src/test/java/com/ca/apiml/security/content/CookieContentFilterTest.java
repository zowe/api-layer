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
import com.ca.apiml.security.token.TokenAuthentication;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.util.Optional;

public class CookieContentFilterTest {

    private CookieContentFilter cookieContentFilter;
    private AuthenticationManager authenticationManager;
    private AuthenticationFailureHandler failureHandler;
    private SecurityConfigurationProperties securityConfigurationProperties;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        securityConfigurationProperties = new SecurityConfigurationProperties();
        request = new MockHttpServletRequest();
        cookieContentFilter = new CookieContentFilter(authenticationManager, failureHandler, securityConfigurationProperties);
    }

    @Test
    public void shouldReturnEmptyIfNoCookies() {
        Optional<AbstractAuthenticationToken> content =  cookieContentFilter.extractContent(request);
        assertEquals(Optional.empty(), content);
    }

    @Test
    public void shouldExtractContent() {
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "cookie");
        request.setCookies(cookie);
        Optional<AbstractAuthenticationToken> content =  cookieContentFilter.extractContent(request);
        TokenAuthentication actualToken = new TokenAuthentication(cookie.getValue());
        assertTrue(content.isPresent());
        assertEquals(content.get(), actualToken);

    }

    @Test
    public void shouldReturnEmptyIfCookieValueIsEmpty() {
        Cookie cookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "");
        request.setCookies(cookie);
        Optional<AbstractAuthenticationToken> content =  cookieContentFilter.extractContent(request);
        assertFalse(content.isPresent());
    }
}
