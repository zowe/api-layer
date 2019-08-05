package com.ca.mfaas.apicatalog.security;

import com.ca.apiml.security.config.AuthConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
import javax.servlet.http.Cookie;

import static org.junit.Assert.*;

public class ApiCatalogLogoutSuccessHandlerTest {

    @Test
    public void testOnLogoutSuccess() {
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        MockHttpSession mockHttpSession = new MockHttpSession();
        httpServletRequest.setSession(mockHttpSession);

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        AuthConfigurationProperties securityConfigurationProperties = new AuthConfigurationProperties();
        ApiCatalogLogoutSuccessHandler apiCatalogLogoutSuccessHandler = new ApiCatalogLogoutSuccessHandler(securityConfigurationProperties);

        apiCatalogLogoutSuccessHandler.onLogoutSuccess(
            httpServletRequest,
            httpServletResponse,
            new TokenAuthentication("TEST_TOKEN_STRING")
        );

        assertTrue(mockHttpSession.isInvalid());
        assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());

        Cookie cookie = httpServletResponse.getCookie(
            securityConfigurationProperties.getCookieProperties().getCookieName());
        assertNotNull(cookie);
        assertTrue(cookie.getSecure());
        assertTrue(cookie.isHttpOnly());
    }
}
