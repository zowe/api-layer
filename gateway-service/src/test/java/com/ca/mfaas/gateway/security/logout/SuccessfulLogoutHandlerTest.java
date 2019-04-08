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
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.Cookie;

import static org.junit.Assert.*;

public class SuccessfulLogoutHandlerTest {

    private SuccessfulLogoutHandler successfulLogoutHandler; // = new SuccessfulLogoutHandler();
    private SecurityConfigurationProperties securityConfigurationProperties = new SecurityConfigurationProperties();
    private MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
    private MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
    private SecurityContext securityContext = SecurityContextHolder.getContext();

    @Test
    public void shouldClearCookieValue() throws Exception {

        Cookie tokenCookie = new Cookie(securityConfigurationProperties.getCookieProperties().getCookieName(), "TEST_WOOKIE");
        tokenCookie.setPath(securityConfigurationProperties.getCookieProperties().getCookiePath());
        tokenCookie.setComment(securityConfigurationProperties.getCookieProperties().getCookieComment());
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(1000);
        httpServletRequest.setCookies(tokenCookie);

        SuccessfulLogoutHandler successfulLogoutHandler = new SuccessfulLogoutHandler(securityConfigurationProperties);
        successfulLogoutHandler.onLogoutSuccess(httpServletRequest,httpServletResponse, null);

        Cookie responseCookie = httpServletResponse.getCookie(securityConfigurationProperties.getCookieProperties().getCookieName());
        assertNotNull(responseCookie);
        assertNull(responseCookie.getValue());
    }

    @Test
    public void shouldInvalidateSession() throws Exception {

        MockHttpSession httpSession = new MockHttpSession();
        httpServletRequest.setSession(httpSession);

        assertEquals(false,((MockHttpSession)httpServletRequest.getSession()).isInvalid());
        assertEquals("1",((MockHttpSession)httpServletRequest.getSession()).getId());
        assertEquals(false,httpSession.isInvalid());

        SuccessfulLogoutHandler successfulLogoutHandler = new SuccessfulLogoutHandler(securityConfigurationProperties);
        successfulLogoutHandler.onLogoutSuccess(httpServletRequest,httpServletResponse, null);

        assertEquals(false, ((MockHttpSession) httpServletRequest.getSession()).isInvalid());
        assertEquals("2",((MockHttpSession)httpServletRequest.getSession()).getId());
        assertEquals(true,httpSession.isInvalid());
    }
}
