/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.query;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.Assert.*;

public class SuccessfulQueryHandlerTest {
    private SecurityConfigurationProperties securityConfigurationProperties;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;
    private SuccessfulQueryHandler successfulQueryHandler;
    private ObjectMapper mapper;
    private String jwtToken;
    private AuthenticationService authenticationService;

    public static final String USER = "Me";
    public static final String DOMAIN = "this.com";
    public static final String LTPA = "ltpaToken";

    @Before
    public void setup() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();
        mapper = new ObjectMapper();
        authenticationService = new AuthenticationService(securityConfigurationProperties);
        authenticationService.setSecret("very_secret");
        jwtToken = authenticationService.createJwtToken(USER, DOMAIN, LTPA);

        successfulQueryHandler = new SuccessfulQueryHandler(mapper, authenticationService);
    }

    @Test
    public void shouldSetResponseParameters() throws Exception {
        httpServletResponse = new MockHttpServletResponse();
        TokenAuthentication tokenAuthentication = new TokenAuthentication(USER, jwtToken);
        httpServletResponse.setStatus(HttpStatus.EXPECTATION_FAILED.value());
        assertNotEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());

        successfulQueryHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, httpServletResponse.getContentType());
        assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
    }

    @Test
    public void shouldWriteModelToBody() throws Exception {
        httpServletResponse = new MockHttpServletResponse();
        TokenAuthentication tokenAuthentication = new TokenAuthentication(USER, jwtToken);

        successfulQueryHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, tokenAuthentication);

        assertNotNull(httpServletResponse.getContentAsString());
        String response = httpServletResponse.getContentAsString();

        assertTrue(response.contains("\"domain\":\""+DOMAIN+"\""));
        assertTrue(response.contains("\"userId\":\""+USER+"\""));
        assertTrue(response.contains("\"creation\":"));
        assertTrue(response.contains("\"expiration\":"));
    }
}
