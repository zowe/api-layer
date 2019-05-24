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

import com.ca.mfaas.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.security.query.QueryResponse;
import com.ca.mfaas.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import javax.servlet.http.Cookie;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.Date;

import static com.ca.mfaas.security.token.TokenService.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TokenServiceTest {
    private static final String TEST_TOKEN = "token";
    private static final String TEST_DOMAIN = "domain";
    private static final String TEST_USER = "user";
    private static final String ALGORITHM = "RS256";

    private Key privateKey;
    private PublicKey publicKey;

    private SecurityConfigurationProperties securityConfigurationProperties;
    private TokenService tokenService;

    @Mock
    private JwtSecurityInitializer jwtSecurityInitializer;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        if (keyPair != null) {
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        }
        when(jwtSecurityInitializer.getSignatureAlgorithm()).thenReturn(ALGORITHM);
        when(jwtSecurityInitializer.getJwtSecret()).thenReturn(privateKey);
        when(jwtSecurityInitializer.getJwtPublicKey()).thenReturn(publicKey);

        securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.getTokenProperties().setIssuer("test");
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(60 * 60);
        tokenService = new TokenService(securityConfigurationProperties, jwtSecurityInitializer);
    }


    @Test(expected = IllegalArgumentException.class)
    public void tokenServiceWithoutSecretCannotWork() {
        when(jwtSecurityInitializer.getJwtSecret()).thenReturn(null);
        tokenService.createToken(TEST_USER);
    }

    @Test
    public void createTokenForGeneralUser() {
        String token = tokenService.createToken(TEST_USER);
        Claims claims = Jwts.parser().setSigningKey(publicKey)
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
        String token = tokenService.createToken(expirationUsername);
        Claims claims = Jwts.parser().setSigningKey(publicKey)
                .parseClaimsJws(token).getBody();

        assertThat(claims.getSubject(), is(expirationUsername));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(expiration));
    }

    @Test
    public void createTokenForNonExpirationUser() {
        String token = tokenService.createToken(TEST_USER);
        Claims claims = Jwts.parser().setSigningKey(publicKey)
                .parseClaimsJws(token).getBody();

        assertThat(claims.getSubject(), is(TEST_USER));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(securityConfigurationProperties.getTokenProperties().getExpirationInSeconds()));
    }

    @Test
    public void validateValidToken() {
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

        String token = tokenService.createToken(expirationUser);
        TokenAuthentication authentication = new TokenAuthentication(token);
        tokenService.validateToken(authentication);
    }

    @Test
    public void validateTokenWithWrongSecretSection() {
        String signaturePadding = "someText";
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Token is not valid");

        String token = tokenService.createToken(TEST_USER);
        TokenAuthentication authentication = new TokenAuthentication(token + signaturePadding);
        tokenService.validateToken(authentication);
    }

    @Test
    public void getTokenReturnsTokenInCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("apimlAuthenticationToken", TEST_TOKEN));

        assertEquals(TEST_TOKEN, tokenService.getToken(request));
    }

    @Test
    public void getTokenReturnsTokenInAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_TYPE_PREFIX + " " + TEST_TOKEN);

        assertEquals(TEST_TOKEN, tokenService.getToken(request));
    }

    @Test
    public void getTokenReturnsNullIfTokenIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(tokenService.getToken(request));
    }

    @Test
    public void getLtpaTokenReturnsTokenFromJwt() {
        String jwtToken = Jwts.builder().claim(LTPA_CLAIM_NAME, TEST_TOKEN)
                .signWith(SignatureAlgorithm.forName(ALGORITHM), privateKey)
                .compact();
        assertEquals(TEST_TOKEN, tokenService.getLtpaToken(jwtToken));
    }

    @Test
    public void parseToken() {
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
            .signWith(SignatureAlgorithm.forName(ALGORITHM), privateKey)
            .compact();

        QueryResponse response = new QueryResponse(TEST_DOMAIN, TEST_USER, issuedAt, expiration);

        assertEquals(response.toString(), tokenService.parseToken(jwtToken).toString());
    }

    @Test
    public void getLtpaTokenReturnsNullIfLtpaIsMissing() {
        TokenService tokenService = new TokenService(securityConfigurationProperties, jwtSecurityInitializer);
        String jwtToken = Jwts.builder().claim(DOMAIN_CLAIM_NAME, TEST_DOMAIN)
                .signWith(SignatureAlgorithm.forName(ALGORITHM), privateKey)
                .compact();
        assertNull(tokenService.getLtpaToken(jwtToken));
    }
}

