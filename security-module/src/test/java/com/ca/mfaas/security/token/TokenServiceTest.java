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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import javax.servlet.http.Cookie;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;


public class TokenServiceTest {
    private SecurityConfigurationProperties securityConfigurationProperties;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        securityConfigurationProperties = new SecurityConfigurationProperties();
        securityConfigurationProperties.getTokenProperties().setSecret("secret");
        securityConfigurationProperties.getTokenProperties().setIssuer("test");
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(60 * 60);
    }

    @Test
    public void createTokenForGeneralUser() {
        String username = "user";

        TokenService tokenService = new TokenService(securityConfigurationProperties);
        String token = tokenService.createToken(username);
        Claims claims = Jwts.parser()
            .setSigningKey(securityConfigurationProperties.getTokenProperties().getSecret())
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(username));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(securityConfigurationProperties.getTokenProperties().getExpirationInSeconds()));
    }

    @Test
    public void createTokenForExpirationUser() {
        String expirationUsername = "user";
        long expiration = 10;
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(10);
        TokenService tokenService = new TokenService(securityConfigurationProperties);
        String token = tokenService.createToken(expirationUsername);
        Claims claims = Jwts.parser()
            .setSigningKey(securityConfigurationProperties.getTokenProperties().getSecret())
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(expirationUsername));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(expiration));
    }

    @Test
    public void createTokenForNonExpirationUser() {
        String username = "user";

        TokenService tokenService = new TokenService(securityConfigurationProperties);
        String token = tokenService.createToken(username);
        Claims claims = Jwts.parser()
            .setSigningKey(securityConfigurationProperties.getTokenProperties().getSecret())
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(username));
        assertThat(claims.getIssuer(), is(securityConfigurationProperties.getTokenProperties().getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(securityConfigurationProperties.getTokenProperties().getExpirationInSeconds()));
    }

    @Test
    public void validateValidToken() {
        String username = "user";

        TokenService tokenService = new TokenService(securityConfigurationProperties);
        String token = tokenService.createToken(username);
        TokenAuthentication authentication = new TokenAuthentication(token);
        TokenAuthentication validatedAuthentication = tokenService.validateToken(authentication);

        assertThat(validatedAuthentication.isAuthenticated(), is(true));
        assertThat(validatedAuthentication.getPrincipal(), is(username));
    }

    @Test
    public void validateExpiredToken() {
        String expirationUser = "user";
        securityConfigurationProperties.getTokenProperties().setExpirationInSeconds(0);

        exception.expect(TokenExpireException.class);
        exception.expectMessage("is expired");

        TokenService tokenService = new TokenService(securityConfigurationProperties);
        String token = tokenService.createToken(expirationUser);
        TokenAuthentication authentication = new TokenAuthentication(token);
        tokenService.validateToken(authentication);

    }

    @Test
    public void validateTokenWithWrongSecretSection() {
        String username = "user";
        String signaturePadding = "someText";
        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Token is not valid");

        TokenService tokenService = new TokenService(securityConfigurationProperties);
        String token = tokenService.createToken(username);
        TokenAuthentication authentication = new TokenAuthentication(token + signaturePadding);
        tokenService.validateToken(authentication);
    }

    @Test
    public void getTokenReturnsTokenInCookie() {
        TokenService tokenService = new TokenService(securityConfigurationProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("apimlAuthenticationToken", "token"));

        assertEquals("token", tokenService.getToken(request));
    }

    @Test
    public void getTokenReturnsTokenInAuthorizationHeader() {
        TokenService tokenService = new TokenService(securityConfigurationProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");

        assertEquals("token", tokenService.getToken(request));        
    }

    @Test
    public void getTokenReturnsNullIfTokenIsMissing() {
        TokenService tokenService = new TokenService(securityConfigurationProperties);
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertEquals(null, tokenService.getToken(request));
    }    
}
