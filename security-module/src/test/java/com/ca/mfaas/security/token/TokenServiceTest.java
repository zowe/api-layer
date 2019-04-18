/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.token;

import static com.ca.mfaas.security.token.TokenService.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.servlet.http.Cookie;

import com.ca.mfaas.product.web.HttpConfig;
import com.ca.mfaas.security.config.SecurityConfigurationProperties;

import com.ca.mfaas.security.query.QueryResponse;
import io.jsonwebtoken.SignatureException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Calendar;
import java.util.Date;

public class TokenServiceTest {
    private static final String TEST_TOKEN = "token";
    private static final String TEST_DOMAIN = "domain";
    private static final String TEST_USER = "user";
    private static final String SECRET = "secret";

    private SecurityConfigurationProperties securityConfigurationProperties;

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private HttpConfig httpConfig;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.getTokenProperties().setIssuer("test");
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(60 * 60);
        tokenService = new TokenService(securityConfigurationProperties);
        MockitoAnnotations.initMocks(this);
    }


    @Test(expected = NullPointerException.class)
    public void tokenServiceWithoutSecretCannotWork() {
        tokenService = new TokenService(securityConfigurationProperties);
        tokenService.createToken(TEST_USER);
    }

    @Test
    public void createTokenForGeneralUser() {
        tokenService.setSecret(SECRET);
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512");
        String token = tokenService.createToken(TEST_USER);
        Claims claims = Jwts.parser().setSigningKey(tokenService.getSecret())
                .parseClaimsJws(token).getBody();

        assertThat(claims.getSubject(), is(TEST_USER));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(securityConfigurationProperties.getTokenProperties().getExpirationInSeconds()));
    }

    @Test
    public void createTokenForExpirationUser() {
        String expirationUsername = "user";
        long expiration = 10;
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(10);
        tokenService.setSecret(SECRET);
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512");
        String token = tokenService.createToken(expirationUsername);
        Claims claims = Jwts.parser().setSigningKey(tokenService.getSecret())
                .parseClaimsJws(token).getBody();

        assertThat(claims.getSubject(), is(expirationUsername));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(expiration));
    }

    @Test
    public void createTokenForNonExpirationUser() {
        tokenService.setSecret(SECRET);
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512");
        String token = tokenService.createToken(TEST_USER);
        Claims claims = Jwts.parser().setSigningKey(tokenService.getSecret())
                .parseClaimsJws(token).getBody();

        assertThat(claims.getSubject(), is(TEST_USER));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(securityConfigurationProperties.getTokenProperties().getExpirationInSeconds()));
    }

    @Test
    public void validateValidToken() {
        tokenService.setSecret(SECRET);
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512");
        String token = tokenService.createToken(TEST_USER);
        TokenAuthentication authentication = new TokenAuthentication(token);
        TokenAuthentication validatedAuthentication = tokenService.validateToken(authentication);

        assertThat(validatedAuthentication.isAuthenticated(), is(true));
        assertThat(validatedAuthentication.getPrincipal(), is(TEST_USER));
    }

    @Test
    public void validateExpiredToken() {
        String expirationUser = "user";
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(0);

        exception.expect(TokenExpireException.class);
        exception.expectMessage("is expired");

        tokenService.setSecret(SECRET);
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512");
        String token = tokenService.createToken(expirationUser);
        TokenAuthentication authentication = new TokenAuthentication(token);
        tokenService.validateToken(authentication);
    }

    @Test
    public void validateTokenWithWrongSecretSection() {
        String signaturePadding = "someText";
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Token is not valid");

        tokenService.setSecret(SECRET);
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512");
        String token = tokenService.createToken(TEST_USER);
        TokenAuthentication authentication = new TokenAuthentication(token + signaturePadding);
        tokenService.validateToken(authentication);
    }

    @Test
    public void getTokenReturnsTokenInCookie() {
        tokenService.setSecret(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("apimlAuthenticationToken", TEST_TOKEN));

        assertEquals(TEST_TOKEN, tokenService.getToken(request));
    }

    @Test
    public void getTokenReturnsTokenInAuthorizationHeader() {
        tokenService.setSecret(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TYPE_PREFIX + " " + TEST_TOKEN);

        assertEquals(TEST_TOKEN, tokenService.getToken(request));
    }

    @Test
    public void getTokenReturnsNullIfTokenIsMissing() {
        tokenService.setSecret(SECRET);
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertEquals(null, tokenService.getToken(request));
    }

    @Test
    public void getLtpaTokenReturnsTokenFromJwt() {
        tokenService.setSecret(SECRET);
        String jwtToken = Jwts.builder().claim(LTPA_CLAIM_NAME, TEST_TOKEN)
                .signWith(SignatureAlgorithm.HS512, tokenService.getSecret())
                .compact();
        assertEquals(TEST_TOKEN, tokenService.getLtpaToken(jwtToken));
    }

    @Test
    public void parseToken() {
        tokenService.setSecret(SECRET);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2018, 1, 15);
        Date issuedAt = calendar.getTime();
        calendar.set(2099, 1, 16);
        Date expiration = calendar.getTime();

        String jwtToken = Jwts.builder()
            .setSubject(TEST_USER)
            .claim(DOMAIN_CLAIM_NAME, TEST_DOMAIN)
            .setIssuedAt(issuedAt)
            .setExpiration(expiration)
            .setIssuer(securityConfigurationProperties.getTokenProperties().getIssuer())
            .signWith(SignatureAlgorithm.HS512, tokenService.getSecret())
            .compact();

        QueryResponse response = new QueryResponse(TEST_DOMAIN, TEST_USER, issuedAt, expiration);

        assertEquals(response.toString(), tokenService.parseToken(jwtToken).toString());
    }

    @Test
    public void getLtpaTokenReturnsNullIfLtpaIsMissing() {
        tokenService.setSecret(SECRET);
        String jwtToken = Jwts.builder().claim(DOMAIN_CLAIM_NAME, TEST_DOMAIN)
                .signWith(SignatureAlgorithm.HS512, tokenService.getSecret())
                .compact();
        assertEquals(null, tokenService.getLtpaToken(jwtToken));
    }

    @Test
    public void shouldThrowExceptionWhenUsingUnsupportedSignAlgorithm() {
        tokenService.setSecret(SECRET);
        exception.expect(SignatureException.class);
        exception.expectMessage("Unsupported signature algorithm 'HS512d'");
        when(httpConfig.getJwtSignatureAlgorithm()).thenReturn("HS512d");
        tokenService.createToken(TEST_USER);
    }
}
