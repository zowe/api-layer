/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.metrics.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;

class MetricsServiceLogoutSuccessHandlerTest {

    private MockHttpServletRequest mockHttpServletRequest;
    private MockHttpServletResponse mockHttpServletResponse;
    private MockHttpSession mockHttpSession;
    private AuthConfigurationProperties authConfigurationProperties;
    private MetricsServiceLogoutSuccessHandler underTest;

    @BeforeEach
    void setup() {
        mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletResponse = new MockHttpServletResponse();
        mockHttpSession = new MockHttpSession();

        authConfigurationProperties = new AuthConfigurationProperties();
        underTest = new MetricsServiceLogoutSuccessHandler(authConfigurationProperties);
    }

    @Test
    void givenHttpSession_whenLogoutHandled_thenInvalidateSessionAndSetCookie() {
        mockHttpServletRequest.setSession(mockHttpSession);

        underTest.onLogoutSuccess(
            mockHttpServletRequest,
            mockHttpServletResponse,
            new TokenAuthentication("TEST_TOKEN_STRING")
        );

        assertTrue(mockHttpSession.isInvalid());
        assertEquals(HttpStatus.OK.value(), mockHttpServletResponse.getStatus());

        Cookie cookie = mockHttpServletResponse.getCookie(
            authConfigurationProperties.getCookieProperties().getCookieName());
        assertNotNull(cookie);
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
