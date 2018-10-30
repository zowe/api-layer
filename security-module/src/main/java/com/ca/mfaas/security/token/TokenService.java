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

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class TokenService {
    private final TokenServiceConfiguration serviceConfiguration;

    public TokenService(TokenServiceConfiguration serviceConfiguration) {
        this.serviceConfiguration = serviceConfiguration;
    }

    public String createToken(String username) {
        long now = System.currentTimeMillis();
        long expiration = calculateExpiration(now, username);

        String token = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer(serviceConfiguration.getIssuer())
            .setId(UUID.randomUUID().toString())
            .signWith(SignatureAlgorithm.HS512, serviceConfiguration.getSecret())
            .compact();

        return token;
    }

    public TokenAuthentication validateToken(TokenAuthentication token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(serviceConfiguration.getSecret())
                .parseClaimsJws(token.getCredentials())
                .getBody();

            claims.getExpiration();
            String username = claims.getSubject();
            TokenAuthentication validTokenAuthentication = new TokenAuthentication(username, token.getCredentials());
            validTokenAuthentication.setAuthenticated(true);
            return validTokenAuthentication;
        } catch (ExpiredJwtException exception) {
            log.debug("Token with id '{}' for user '{}' is expired", exception.getClaims().getId(), exception.getClaims().getSubject());
            throw new TokenExpireException("Token is expired");
        } catch (JwtException exception) {
            log.debug("Token is not valid due to: {}", exception.getMessage());
            throw new TokenNotValidException("Token is not valid");
        } catch (Exception exception) {
            log.debug("Token is not valid due to: {}", exception.getMessage());
            throw new TokenNotValidException("An internal error occurred while validating the token therefor the token is no longer valid");
        }
    }

    private long calculateExpiration(long now, String username) {
        long expiration = now + (serviceConfiguration.getExpirationInSeconds() * 1000);
        // calculate time for short TTL user
        if (serviceConfiguration.getShortTtlUsername() != null) {
            if (username.equals(serviceConfiguration.getShortTtlUsername())) {
                expiration = now + (serviceConfiguration.getShortTtlExpiration() * 1000);
            }
        }
        return expiration;
    }
}
