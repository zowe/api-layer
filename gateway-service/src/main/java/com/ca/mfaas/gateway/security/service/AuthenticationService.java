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

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.apiml.security.common.token.TokenAuthentication;
import com.ca.apiml.security.common.token.TokenExpireException;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.constants.ApimlConstants;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Service for the JWT and LTPA tokens operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Scope( proxyMode = ScopedProxyMode.TARGET_CLASS )
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthenticationService {
    private static final String LTPA_CLAIM_NAME = "ltpa";
    private static final String DOMAIN_CLAIM_NAME = "dom";

    private final ApplicationContext applicationContext;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final JwtSecurityInitializer jwtSecurityInitializer;
    private final EurekaClient discoveryClient;
    private final RestTemplate restTemplate;

    // to force calling inside methods with aspects - ie. ehCache aspect
    private AuthenticationService meAsProxy;

    @PostConstruct
    public void afterPropertiesSet() {
        meAsProxy = applicationContext.getBean(AuthenticationService.class);
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
            .setIssuer(authConfigurationProperties.getTokenProperties().getIssuer())
            .setId(UUID.randomUUID().toString())
            .signWith(jwtSecurityInitializer.getSignatureAlgorithm(), jwtSecurityInitializer.getJwtSecret())
            .compact();
    }

    /**
     * Method will invalidate jwtToken. It could be called from two reasons:
     *  - on logout phase (distribute = true)
     *  - from another gateway instance to notify about change (distribute = false)
     * @param jwtToken token to invalidated
     * @param distribute distribute invalidation to another instances?
     * @return state of invalidate (true - token was invalidated)
     */
    @CacheEvict(value = "validationJwtToken", key = "#jwtToken")
    @Cacheable(value = "invalidatedJwtTokens", key = "#jwtToken", condition = "#jwtToken != null")
    public Boolean invalidateJwtToken(String jwtToken, boolean distribute) {
        /*
         * until ehCache is not distributed, send to other instances invalidation request
         */
        if (distribute) {
            final String myInstanceId = discoveryClient.getApplicationInfoManager().getInfo().getInstanceId();
            for (final InstanceInfo instanceInfo : (List<InstanceInfo>) discoveryClient.getInstancesById("gateway")) {
                if (StringUtils.equals(myInstanceId, instanceInfo.getInstanceId())) continue;

                final String url;
                if (instanceInfo.getSecurePort() == 0) {
                    url = "http://" + instanceInfo.getIPAddr() + ":" + instanceInfo.getPort() + "/auth/invalidate/{}";
                } else {
                    url = "https://" + instanceInfo.getIPAddr() + ":" + instanceInfo.getSecurePort() + "/auth/invalidate/{}";
                }
                restTemplate.delete(url, jwtToken);
            }
        }

        return Boolean.TRUE;
    }

    @Cacheable(value = "invalidatedJwtTokens", unless = "true", key = "#jwtToken", condition = "#jwtToken != null")
    public Boolean isInvalidated(String jwtToken) {
        return Boolean.FALSE;
    }

    @Cacheable(value = "validationJwtToken", key = "#jwtToken", condition = "#jwtToken != null")
    public TokenAuthentication validateJwtToken(String jwtToken) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
                .parseClaimsJws(jwtToken)
                .getBody();

            TokenAuthentication validTokenAuthentication = new TokenAuthentication(claims.getSubject(), jwtToken);
            // without a proxy cache aspect is not working, thus it is necessary get bean from application context
            final boolean authenticated = !meAsProxy.isInvalidated(jwtToken);
            validTokenAuthentication.setAuthenticated(authenticated);

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
     * Validate the JWT token
     *
     * @param token the JWT token
     * @return the {@link TokenAuthentication} object containing username and valid JWT token
     * @throws TokenExpireException   if the token is expired
     * @throws TokenNotValidException if the token is not valid
     */
    public TokenAuthentication validateJwtToken(TokenAuthentication token) {
        return validateJwtToken(Optional.ofNullable(token).map(TokenAuthentication::getCredentials).orElse(null));
    }

    /**
     * Parse the JWT token and return a {@link QueryResponse} object containing the domain, user id, date of creation and date of expiration
     *
     * @param token the JWT token
     * @return the query response
     */
    public QueryResponse parseJwtToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
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
            .filter(cookie -> cookie.getName().equals(authConfigurationProperties.getCookieProperties().getCookieName()))
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
                .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
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
        long expiration = now + (authConfigurationProperties.getTokenProperties().getExpirationInSeconds() * 1000);

        // calculate time for short TTL user
        if (authConfigurationProperties.getTokenProperties().getShortTtlUsername() != null
            && username.equals(authConfigurationProperties.getTokenProperties().getShortTtlUsername())) {
            expiration = now + (authConfigurationProperties.getTokenProperties().getShortTtlExpirationInSeconds() * 1000);
        }

        return expiration;
    }

}
