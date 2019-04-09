/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.query.QueryResponse;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.util.Date;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;


public class AuthenticationServiceTest {

    private String user = "Me";
    private String domain = "this.com";
    private String ltpa = "ltpaToken";
    @Mock
    private AuthenticationService authService;
    @Mock
    private SecurityConfigurationProperties securityConfigurationProperties;

    @Before
    public void setUp() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        authService = new AuthenticationService(securityConfigurationProperties);
        authService.setSecret("very_secret");
    }

    @Test
    public void shouldCreateJwtToken() {

        String jwtToken = authService.createJwtToken(user, domain, ltpa);

        assertFalse(jwtToken.isEmpty());
        assertEquals(jwtToken.getClass().getName(), "java.lang.String");
    }

    @Test
    public void shouldValidateJwtToken() {

        String jwtToken = authService.createJwtToken(user, domain, ltpa);

        TokenAuthentication token = new TokenAuthentication(jwtToken);
        TokenAuthentication jwtValidation = authService.validateJwtToken(token);

        assertEquals(jwtValidation.getPrincipal(), user);
        assertEquals(jwtValidation.getCredentials(), jwtToken);
        assertTrue(jwtValidation.isAuthenticated());
    }

    @Test
    public void shouldParseJwtTokenAsQueryResponse() {

        String jwtToken = authService.createJwtToken(user, domain, ltpa);
        String dateNow = new Date().toString().substring(0,16);
        QueryResponse parsedToken = authService.parseJwtToken(jwtToken);

        assertEquals(parsedToken.getClass().getTypeName(), "com.ca.mfaas.gateway.security.query.QueryResponse");
        assertEquals(parsedToken.getDomain(), domain);
        assertEquals(parsedToken.getUserId(), user);
        assertEquals(parsedToken.getCreation().toString().substring(0,16), dateNow);
        Date toBeExpired = DateUtils.addDays(parsedToken.getCreation(), 1);
        assertEquals(parsedToken.getExpiration(), toBeExpired);
    }

    @Test
    public void shouldReadJwtTokenFromRequest() {

        String jwtToken = authService.createJwtToken(user, domain, ltpa);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("apimlAuthenticationToken", jwtToken));

        assertEquals(authService.getJwtTokenFromRequest(request), jwtToken);

    }

    @Test
    public void shouldReadLtpaTokenFromJwtToken() {

        String jwtToken = authService.createJwtToken(user, domain, ltpa);

        assertEquals(authService.getLtpaTokenFromJwtToken(jwtToken), ltpa);
    }

}
