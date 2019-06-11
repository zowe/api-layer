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
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class TokenService {
    static final String LTPA_CLAIM_NAME = "ltpa";
    static final String DOMAIN_CLAIM_NAME = "dom";
    static final String BEARER_TYPE_PREFIX = "Bearer";

    private final SecurityConfigurationProperties securityConfigurationProperties;
    private final JwtSecurityInitializer jwtSecurityInitializer;

    public TokenService(SecurityConfigurationProperties securityConfigurationProperties, JwtSecurityInitializer jwtSecurityInitializer) {
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.jwtSecurityInitializer = jwtSecurityInitializer;
    }

    public String createToken(String username) {
        return createToken(username, "", "");
    }

    public String createToken(String username, String domain, String ltpaToken) {
        long now = System.currentTimeMillis();
        long expiration = calculateExpiration(now, username);

        return Jwts.builder()
            .setSubject(username)
            .claim(DOMAIN_CLAIM_NAME, domain)
            .claim(LTPA_CLAIM_NAME, ltpaToken)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer(securityConfigurationProperties.getTokenProperties().getIssuer())
            .setId(UUID.randomUUID().toString())
            .signWith(jwtSecurityInitializer.getSignatureAlgorithm(), jwtSecurityInitializer.getJwtSecret())
            .compact();
    }

    TokenAuthentication validateToken(TokenAuthentication token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
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

    public QueryResponse parseToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
            .parseClaimsJws(token)
            .getBody();

        return new QueryResponse(claims.get(DOMAIN_CLAIM_NAME, String.class),
            claims.getSubject(), claims.getIssuedAt(), claims.getExpiration());
    }

    public String getLtpaToken(String jwtToken) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
                .parseClaimsJws(jwtToken)
                .getBody();
            return claims.get(LTPA_CLAIM_NAME, String.class);
        } catch (ExpiredJwtException exception) {
            log.debug("Authentication: Token with id '{}' for user '{}' is expired", exception.getClaims().getId(), exception.getClaims().getSubject());
            throw new TokenExpireException("Token is expired");
        } catch (Exception exception) {
            log.debug("Authentication: Token is not valid due to: {}", exception.getMessage());
            throw new TokenNotValidException("Token is not valid");
        }
    }

    public String getToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(securityConfigurationProperties.getCookieProperties().getCookieName())) {
                    return cookie.getValue();
                }
            }
        }

        return extractTokenFromAuthoritationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
    }

    private String extractTokenFromAuthoritationHeader(String header) {
        if (header != null && header.startsWith(BEARER_TYPE_PREFIX)) {
            return header.replaceFirst(BEARER_TYPE_PREFIX + " ", "");
        }

        return null;
    }

    private long calculateExpiration(long now, String username) {
        long expiration = now + (securityConfigurationProperties.getTokenProperties().getExpirationInSeconds() * 1000);

        // calculate time for short TTL user
        if (securityConfigurationProperties.getTokenProperties().getShortTtlUsername() != null) {
            if (username.equals(securityConfigurationProperties.getTokenProperties().getShortTtlUsername())) {
                expiration = now + (securityConfigurationProperties.getTokenProperties().getShortTtlExpirationInSeconds() * 1000);
            }
        }
        return expiration;
    }
}
