/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.login;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.Assert.*;

public class SuccessfulLoginHandlerTest {
    private SecurityConfigurationProperties securityConfigurationProperties;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private SuccessfulLoginHandler successfulLoginHandler;

    @Before
    public void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();

        securityConfigurationProperties = new SecurityConfigurationProperties();
        successfulLoginHandler = new SuccessfulLoginHandler(securityConfigurationProperties);
    }

    @Test
    public void testOnAuthenticationSuccess() {
        successfulLoginHandler.onAuthenticationSuccess(
            httpServletRequest,
            httpServletResponse,
            new TokenAuthentication("TEST_TOKEN_STRING")
        );

        assertEquals(HttpStatus.NO_CONTENT.value(), httpServletResponse.getStatus());
        assertNotNull(httpServletResponse.getCookie(securityConfigurationProperties.getCookieProperties().getCookieName()));
    }
}
