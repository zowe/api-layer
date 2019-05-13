/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.login;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.login.SuccessfulLoginHandler;
import com.ca.apiml.security.token.TokenAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

        ObjectMapper mapper = new ObjectMapper();
        securityConfigurationProperties = new SecurityConfigurationProperties();
        successfulLoginHandler = new SuccessfulLoginHandler(mapper,securityConfigurationProperties);
    }

    @Test
    public void shouldSetResponseParameters() throws Exception {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("TEST_TOKEN_STRING");
        httpServletResponse.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        assertNotEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());

        successfulLoginHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.NO_CONTENT.value(), httpServletResponse.getStatus());
    }

    @Test
    public void shouldSetResponseCookie() throws Exception {
        TokenAuthentication tokenAuthentication = new TokenAuthentication("TEST_TOKEN_STRING");
        successfulLoginHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);
        assertNotNull(httpServletResponse.getCookie(securityConfigurationProperties.getCookieProperties().getCookieName()));
    }
}
