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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TokenServiceTest {
    private TokenServiceConfiguration tokenServiceConfiguration;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        tokenServiceConfiguration = TokenServiceConfiguration
            .builder()
            .secret("secret")
            .issuer("test")
            .expirationInSeconds(60 * 60)
            .build();
    }

    @Test
    public void createTokenForGeneralUser() {
        String username = "user";

        TokenService tokenService = new TokenService(tokenServiceConfiguration);
        String token = tokenService.createToken(username);
        Claims claims = Jwts.parser()
            .setSigningKey(tokenServiceConfiguration.getSecret())
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(username));
        assertThat(claims.getIssuer(), is(tokenServiceConfiguration.getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(tokenServiceConfiguration.getExpirationInSeconds()));
    }

    @Test
    public void createTokenForExpirationUser() {
        String expirationUsername = "user";
        long expiration = 10;

        TokenServiceConfiguration configuration = tokenServiceConfiguration.toBuilder()
            .shortTtlUsername(expirationUsername)
            .shortTtlExpiration(expiration).build();

        TokenService tokenService = new TokenService(configuration);
        String token = tokenService.createToken(expirationUsername);
        Claims claims = Jwts.parser()
            .setSigningKey(configuration.getSecret())
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(expirationUsername));
        assertThat(claims.getIssuer(), is(configuration.getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(expiration));
    }

    @Test
    public void createTokenForNonExpirationUser() {
        String username = "user";
        String expirationUser = "expire";
        long expiration = 10;

        TokenServiceConfiguration configuration = tokenServiceConfiguration.toBuilder()
            .shortTtlUsername(expirationUser)
            .shortTtlExpiration(expiration).build();

        TokenService tokenService = new TokenService(configuration);
        String token = tokenService.createToken(username);
        Claims claims = Jwts.parser()
            .setSigningKey(configuration.getSecret())
            .parseClaimsJws(token)
            .getBody();

        assertThat(claims.getSubject(), is(username));
        assertThat(claims.getIssuer(), is(configuration.getIssuer()));
        long ttl = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000L;
        assertThat(ttl, is(tokenServiceConfiguration.getExpirationInSeconds()));
    }

    @Test
    public void validateValidToken() {
        String username = "user";

        TokenService tokenService = new TokenService(tokenServiceConfiguration);
        String token = tokenService.createToken(username);
        TokenAuthentication authentication = new TokenAuthentication(token);
        TokenAuthentication validatedAuthentication = tokenService.validateToken(authentication);

        assertThat(validatedAuthentication.isAuthenticated(), is(true));
        assertThat(validatedAuthentication.getPrincipal(), is(username));
    }

    @Test
    public void validateExpiredToken() {
        String expirationUser = "expire";
        long expiration = 0;
        TokenServiceConfiguration configuration = tokenServiceConfiguration.toBuilder()
            .shortTtlUsername(expirationUser)
            .shortTtlExpiration(expiration).build();

        exception.expect(BadCredentialsException.class);
        exception.expectMessage("Token is expired");

        TokenService tokenService = new TokenService(configuration);
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

        TokenService tokenService = new TokenService(tokenServiceConfiguration);
        String token = tokenService.createToken(username);
        TokenAuthentication authentication = new TokenAuthentication(token + signaturePadding);
        tokenService.validateToken(authentication);
    }

}
