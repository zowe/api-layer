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
import com.ca.mfaas.constants.ApimlConstants;
import com.ca.mfaas.gateway.security.query.QueryResponse;
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for the JWT and LTPA tokens operations
 */
@Service
@Slf4j
public class AuthenticationService {
    private static final String LTPA_CLAIM_NAME = "ltpa";
    private static final String DOMAIN_CLAIM_NAME = "dom";

    private final SecurityConfigurationProperties securityConfigurationProperties;
    private String secret;

    private final JwtSecurityInitializer jwtSecurityInitializer;

    public AuthenticationService(SecurityConfigurationProperties securityConfigurationProperties,
                                 JwtSecurityInitializer jwtSecurityInitializer) {
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.jwtSecurityInitializer = jwtSecurityInitializer;
    }

    /**
     * Return the secret key for the JWT token
     *
     * @return the secret key
     * @throws NullPointerException if the secret key is null
     */
    private String getSecret() {
        if (secret == null) {
            throw new NullPointerException("The secret key for JWT token service is null.");
        }

        return secret;
    }

    /**
     * Set the secret key for the JWT token
     *
     * @param secret the secret key
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Create the JWT token and set the LTPA token, the expiration time, the domain, the subject, the date of issue, the issuer and the id.
     * Sign the token with HMAC SHA512 algorithm, using the secret key
     *
     * @param username  the username
     * @param domain    the domain
     * @param ltpaToken the LTPA token
     * @return the JWT token
     */
    public String createJwtToken(String username, String domain, String ltpaToken) {
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
            .signWith(SignatureAlgorithm.forName(jwtSecurityInitializer.getSignatureAlgorithm()), jwtSecurityInitializer.getInitializedSecret())
            .compact();
    }

    /**
     * Validate the JWT token
     *
     * @param token the JWT token
     * @return the {@link TokenAuthentication} object containing username and valid JWT token
     * @throws TokenExpireException   if the token is expired
     * @throws TokenNotValidException if the token is not valid
     */
    public TokenAuthentication validateJwtToken(TokenAuthentication token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(getSecret())
                .parseClaimsJws(token.getCredentials())
                .getBody();

            TokenAuthentication validTokenAuthentication = new TokenAuthentication(claims.getSubject(), token.getCredentials());
            validTokenAuthentication.setAuthenticated(true);

            return validTokenAuthentication;
        } catch (ExpiredJwtException exception) {
            log.debug("Token with id '{}' for user '{}' is expired.", exception.getClaims().getId(), exception.getClaims().getSubject());
            throw new TokenExpireException("Token is expired.");
        } catch (JwtException exception) {
            log.debug("Token is not valid due to: {}.", exception.getMessage());
            throw new TokenNotValidException("Token is not valid.");
        } catch (Exception exception) {
            log.debug("Token is not valid due to: {}.", exception.getMessage());
            throw new TokenNotValidException("An internal error occurred while validating the token therefor the token is no longer valid.");
        }
    }

    /**
     * Parse the JWT token and return a {@link QueryResponse} object containing the domain, user id, date of creation and date of expiration
     *
     * @param token the JWT token
     * @return the query response
     */
    public QueryResponse parseJwtToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecurityInitializer.getInitializedSecret())
            .parseClaimsJws(token)
            .getBody();

        return new QueryResponse(
            claims.get(DOMAIN_CLAIM_NAME, String.class),
            claims.getSubject(),
            claims.getIssuedAt(),
            claims.getExpiration());
    }

    /**
     * Get the JWT token from the authorization header in the http request
     * <p>
     * Order:
     * 1. Authorization header
     * 2. Cookie
     *
     * @param request the http request
     * @return the JWT token
     */
    public Optional<String> getJwtTokenFromRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return extractJwtTokenFromAuthorizationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        }

        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(securityConfigurationProperties.getCookieProperties().getCookieName()))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(Cookie::getValue);
    }

    /**
     * Get the LTPA token from the JWT token
     *
     * @param jwtToken the JWT token
     * @return the LTPA token
     * @throws TokenNotValidException if the JWT token is not valid
     */
    public String getLtpaTokenFromJwtToken(String jwtToken) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(getSecret())
                .parseClaimsJws(jwtToken)
                .getBody();

            return claims.get(LTPA_CLAIM_NAME, String.class);
        } catch (ExpiredJwtException exception) {
            log.debug("Authentication: Token with id '{}' for user '{}' is expired", exception.getClaims().getId(), exception.getClaims().getSubject());
            throw new TokenExpireException("Token is expired");
        } catch (JwtException exception) {
            log.debug("Authentication: Token is not valid due to: {}", exception.getMessage());
            throw new TokenNotValidException("Token is not valid");
        }
    }

    /**
     * Extract the JWT token from the authorization header
     *
     * @param header the http request header
     * @return the JWT token
     */
    private Optional<String> extractJwtTokenFromAuthorizationHeader(String header) {
        if (header != null && header.startsWith(ApimlConstants.BEARER_AUTHENTICATION_PREFIX)) {
            header = header.replaceFirst(ApimlConstants.BEARER_AUTHENTICATION_PREFIX, "").trim();
            if (header.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(header);
        }

        return Optional.empty();
    }

    /**
     * Calculate the expiration time
     *
     * @param now      the current time
     * @param username the username
     * @return the calculated expiration time
     */
    private long calculateExpiration(long now, String username) {
        long expiration = now + (securityConfigurationProperties.getTokenProperties().getExpirationInSeconds() * 1000);

        // calculate time for short TTL user
        if (securityConfigurationProperties.getTokenProperties().getShortTtlUsername() != null
            && username.equals(securityConfigurationProperties.getTokenProperties().getShortTtlUsername())) {
            expiration = now + (securityConfigurationProperties.getTokenProperties().getShortTtlExpirationInSeconds() * 1000);
        }

        return expiration;
    }
}
