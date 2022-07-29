/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;

class SuccessfulLoginHandlerTest {
    private final TokenAuthentication dummyAuth = new TokenAuthentication("TEST_TOKEN_STRING");

    private AuthConfigurationProperties authConfigurationProperties;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private SuccessfulLoginHandler successfulLoginHandler;

    @BeforeEach
    void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();

        authConfigurationProperties = new AuthConfigurationProperties();
        successfulLoginHandler = new SuccessfulLoginHandler(authConfigurationProperties);
    }

    @Nested
    class WhenAuthenticationIsSuccessful {
        @Test
        void givenAuthentication_thenReturn204AndCookie() {
            executeLoginHandler();

            assertEquals(HttpStatus.NO_CONTENT.value(), httpServletResponse.getStatus());

            Cookie cookie = httpServletResponse.getCookie(authConfigurationProperties.getCookieProperties().getCookieName());
            assertNotNull(cookie);

            AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();
            assertEquals(cp.getCookieName(), cookie.getName());
            assertEquals(dummyAuth.getCredentials(), cookie.getValue());
            assertEquals(cp.getCookiePath(), cookie.getPath());
            assertEquals(-1, cookie.getMaxAge());
            assertTrue(cookie.isHttpOnly());
            assertTrue(cookie.getSecure());
        }

        @Test
        void givenCookieSecureNotSet_thenReturnCookieWithoutSecure() {
            authConfigurationProperties.getCookieProperties().setCookieSecure(false);
            executeLoginHandler();

            Cookie cookie = httpServletResponse.getCookie(authConfigurationProperties.getCookieProperties().getCookieName());
            assertNotNull(cookie);
            assertFalse(cookie.getSecure());
        }
    }

    private void executeLoginHandler() {
        successfulLoginHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, dummyAuth);
    }
}
