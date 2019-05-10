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
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;


public class AuthenticationServiceTest {

    private static final String USER = "Me";
    private static final String DOMAIN = "this.com";
    private static final String LTPA = "ltpaToken";
    private static final String SECRET = "very_secret";

    private AuthenticationService authService;

    private SecurityConfigurationProperties securityConfigurationProperties;

    @Before
    public void setUp() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        authService = new AuthenticationService(securityConfigurationProperties);
        authService.setSecret(SECRET);
    }

    @Test
    public void shouldCreateJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        assertFalse(jwtToken.isEmpty());
        assertEquals("java.lang.String", jwtToken.getClass().getName());
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWithNullSecret() {
        authService.setSecret(null);
        authService.createJwtToken(USER, DOMAIN, LTPA);
    }

    @Test
    public void shouldValidateJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        TokenAuthentication token = new TokenAuthentication(jwtToken);
        TokenAuthentication jwtValidation = authService.validateJwtToken(token);

        assertEquals(USER, jwtValidation.getPrincipal());
        assertEquals(jwtValidation.getCredentials(), jwtToken);
        assertTrue(jwtValidation.isAuthenticated());
    }

    @Test(expected = TokenNotValidException.class)
    public void shouldThrowExceptionWhenTokenIsInvalid() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        String brokenToken = jwtToken + "not";
        TokenAuthentication token = new TokenAuthentication(brokenToken);
        authService.validateJwtToken(token);
    }

    @Test(expected = TokenExpireException.class)
    public void shouldThrowExceptionWhenTokenIsExpired() {
        TokenAuthentication token = new TokenAuthentication(createExpiredJwtToken(SECRET));
        authService.validateJwtToken(token);
    }

    @Test(expected = TokenNotValidException.class)
    public void shouldThrowExceptionWhenOccurUnexpectedException() {
        authService.validateJwtToken(null);
    }

    @Test
    public void shouldParseJwtTokenAsQueryResponse() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);

        String dateNow = new Date().toString().substring(0,16);
        QueryResponse parsedToken = authService.parseJwtToken(jwtToken);

        assertEquals("com.ca.mfaas.gateway.security.query.QueryResponse", parsedToken.getClass().getTypeName());
        assertEquals(DOMAIN, parsedToken.getDomain());
        assertEquals(USER, parsedToken.getUserId());
        assertEquals(parsedToken.getCreation().toString().substring(0,16), dateNow);
        Date toBeExpired = DateUtils.addDays(parsedToken.getCreation(), 1);
        assertEquals(parsedToken.getExpiration(), toBeExpired);
    }

    @Test
    public void shouldReadJwtTokenFromRequestCookie() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        MockHttpServletRequest request = new MockHttpServletRequest();

        Optional<String> optionalToken = authService.getJwtTokenFromRequest(request);
        assertFalse(optionalToken.isPresent());

        request.setCookies(new Cookie("apimlAuthenticationToken", jwtToken));

        optionalToken = authService.getJwtTokenFromRequest(request);
        assertTrue(optionalToken.isPresent());
        assertEquals(optionalToken.get(), jwtToken);
    }

    @Test
    public void shouldExtractJwtFromRequestHeader() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        Optional<String> optionalToken = authService.getJwtTokenFromRequest(request);
        assertFalse(optionalToken.isPresent());

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", String.format("Bearer %s", jwtToken));
        optionalToken = authService.getJwtTokenFromRequest(request);
        assertTrue(optionalToken.isPresent());
        assertEquals(optionalToken.get(), jwtToken);
    }

    @Test
    public void shouldReadLtpaTokenFromJwtToken() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        assertEquals(LTPA, authService.getLtpaTokenFromJwtToken(jwtToken));
    }

    @Test(expected = TokenNotValidException.class)
    public void shouldThrowExceptionWhenTokenIsInvalidWhileExtractingLtpa() {
        String jwtToken = authService.createJwtToken(USER, DOMAIN, LTPA);
        String brokenToken = jwtToken + "not";
        authService.getLtpaTokenFromJwtToken(brokenToken);
    }

    @Test(expected = TokenExpireException.class)
    public void shouldThrowExceptionWhenTokenIsExpiredWhileExtractingLtpa() {
        authService.getLtpaTokenFromJwtToken(createExpiredJwtToken(SECRET));
    }

    private String createExpiredJwtToken(String secret) {
        long expiredTimeMillis = System.currentTimeMillis() - 1000;

        return Jwts.builder()
            .setExpiration(new Date(expiredTimeMillis))
            .setIssuer(securityConfigurationProperties.getTokenProperties().getIssuer())
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
}
