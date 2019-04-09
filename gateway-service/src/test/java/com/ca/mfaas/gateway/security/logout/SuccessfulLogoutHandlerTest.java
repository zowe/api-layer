/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.logout;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;

import static org.junit.Assert.*;

public class SuccessfulLogoutHandlerTest {

    private SecurityConfigurationProperties securityConfigurationProperties;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private SuccessfulLogoutHandler successfulLogoutHandler;


    @Before
    public void setup() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();

        Cookie tokenCookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "TEST_COOKIE");
        tokenCookie.setPath(securityConfigurationProperties.getCookieProperties().getCookiePath());
        tokenCookie.setComment(securityConfigurationProperties.getCookieProperties().getCookieComment());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(1000);
        httpServletRequest.setCookies(tokenCookie);

        successfulLogoutHandler = new SuccessfulLogoutHandler(securityConfigurationProperties);
    }

    @Test
    public void shouldClearCookieValue() throws Exception {
        successfulLogoutHandler.onLogoutSuccess(httpServletRequest,httpServletResponse, null);

        Cookie responseCookie = httpServletResponse.getCookie(securityConfigurationProperties.getCookieProperties().getCookieName());
        assertNotNull(responseCookie);
        assertNull(responseCookie.getValue());
    }

    @Test
    public void shouldInvalidateSession() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();
        httpServletRequest.setSession(httpSession);

        assertFalse(((MockHttpSession)httpServletRequest.getSession()).isInvalid());
        assertEquals("1",((MockHttpSession)httpServletRequest.getSession()).getId());
        assertFalse(httpSession.isInvalid());

        successfulLogoutHandler.onLogoutSuccess(httpServletRequest,httpServletResponse, null);

        assertFalse(((MockHttpSession) httpServletRequest.getSession()).isInvalid());
        assertEquals("2",((MockHttpSession)httpServletRequest.getSession()).getId());
        assertTrue(httpSession.isInvalid());
    }

    @Test
    public void shouldCleanSecurityContextAuthentication() throws Exception {
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        Authentication authentication = new TokenAuthentication("TEST_TOKEN_STRING");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        successfulLogoutHandler.onLogoutSuccess(httpServletRequest,httpServletResponse, null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
