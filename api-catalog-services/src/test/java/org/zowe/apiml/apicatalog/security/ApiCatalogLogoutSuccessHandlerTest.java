/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import org.apache.tomcat.util.http.SameSiteCookies;
import org.junit.jupiter.api.BeforeEach;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.security.common.utils.SecurityUtils.COOKIE_NAME;

class ApiCatalogLogoutSuccessHandlerTest {

    AuthConfigurationProperties authConfigurationProperties = mock(AuthConfigurationProperties.class);

    @BeforeEach
    void setUp() {
        AuthConfigurationProperties.CookieProperties cookieProperties = mock(AuthConfigurationProperties.CookieProperties.class);
        when(authConfigurationProperties.getCookieProperties()).thenReturn(cookieProperties);
        authConfigurationProperties.getCookieProperties().setCookieComment("");
        authConfigurationProperties.getCookieProperties().setCookieNamePAT("PAT");
        authConfigurationProperties.getCookieProperties().setCookieMaxAge(null);
        authConfigurationProperties.getCookieProperties().setCookieSecure(true);
        authConfigurationProperties.getCookieProperties().setCookieSameSite(SameSiteCookies.STRICT);
        when(authConfigurationProperties.getCookieProperties().getCookieSameSite()).thenReturn(SameSiteCookies.STRICT);
        when(authConfigurationProperties.getCookieProperties().getCookiePath()).thenReturn("/");
        when(authConfigurationProperties.getCookieProperties().isCookieSecure()).thenReturn(true);
        when(authConfigurationProperties.getCookieProperties().getCookieMaxAge()).thenReturn(null);
        when(authConfigurationProperties.getCookieProperties().getCookieName()).thenReturn(COOKIE_NAME);
    }

    @Test
    void testOnLogoutSuccess() {
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        MockHttpSession mockHttpSession = new MockHttpSession();
        httpServletRequest.setSession(mockHttpSession);

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        ApiCatalogLogoutSuccessHandler apiCatalogLogoutSuccessHandler = new ApiCatalogLogoutSuccessHandler(authConfigurationProperties);

        apiCatalogLogoutSuccessHandler.onLogoutSuccess(
            httpServletRequest,
            httpServletResponse,
            new TokenAuthentication("TEST_TOKEN_STRING")
        );

        assertTrue(mockHttpSession.isInvalid());
        assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());

        Cookie cookie = httpServletResponse.getCookie(
            authConfigurationProperties.getCookieProperties().getCookieName());
        assertNotNull(cookie);
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
