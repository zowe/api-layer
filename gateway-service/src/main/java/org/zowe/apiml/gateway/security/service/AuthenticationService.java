/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.controllers.AuthController;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.*;
import org.zowe.apiml.util.CacheUtils;
import org.zowe.apiml.util.EurekaUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.LTPA;

/**
 * Service for the JWT and LTPA tokens operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuthenticationService {

    private static final String LTPA_CLAIM_NAME = "ltpa";
    private static final String DOMAIN_CLAIM_NAME = "dom";
    private static final String CACHE_VALIDATION_JWT_TOKEN = "validationJwtToken";
    private static final String CACHE_INVALIDATED_JWT_TOKENS = "invalidatedJwtTokens";

    private static final String TOKEN_IS_NOT_VALID_DUE_TO = "Token is not valid due to: {}.";

    private final ApplicationContext applicationContext;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final JwtSecurityInitializer jwtSecurityInitializer;
    private final ZosmfService zosmfService;
    private final EurekaClient discoveryClient;
    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;
    private final CacheUtils cacheUtils;

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
            .signWith(jwtSecurityInitializer.getJwtSecret(), jwtSecurityInitializer.getSignatureAlgorithm())
            .compact();
    }

    /**
     * Method will invalidate jwtToken. It could be called from two reasons:
     * - on logout phase (distribute = true)
     * - from another gateway instance to notify about change (distribute = false)
     *
     * @param jwtToken   token to invalidate
     * @param distribute distribute invalidation to another instances?
     * @return state of invalidate (true - token was invalidated)
     */
    @CacheEvict(value = CACHE_VALIDATION_JWT_TOKEN, key = "#jwtToken")
    @Cacheable(value = CACHE_INVALIDATED_JWT_TOKENS, key = "#jwtToken", condition = "#jwtToken != null")
    public Boolean invalidateJwtToken(String jwtToken, boolean distribute) {
        /*
         * until ehCache is not distributed, send to other instances invalidation request
         */
        if (distribute && !invalidateTokenOnAnotherInstance(jwtToken)) {
            return Boolean.FALSE;
        }

        // invalidate token in z/OSMF
        final QueryResponse queryResponse = parseJwtToken(jwtToken);
        switch (queryResponse.getSource()) {
            case ZOWE:
                final String ltpaToken = getLtpaToken(jwtToken);
                if (ltpaToken != null) zosmfService.invalidate(LTPA, ltpaToken);
                break;
            case ZOSMF:
                zosmfService.invalidate(JWT, jwtToken);
                break;
            default:
                throw new TokenFormatNotValidException("Unknown token type.");
        }

        return Boolean.TRUE;
    }

    private boolean invalidateTokenOnAnotherInstance(String jwtToken) {
        final Application application = discoveryClient.getApplication(CoreService.GATEWAY.getServiceId());
        // wrong state, gateway have to exists (at least this current instance), return false like unsuccessful
        if (application == null) {
            return Boolean.FALSE;
        }

        final String myInstanceId = discoveryClient.getApplicationInfoManager().getInfo().getInstanceId();
        for (final InstanceInfo instanceInfo : application.getInstances()) {
            if (StringUtils.equals(myInstanceId, instanceInfo.getInstanceId())) continue;

            final String url = EurekaUtils.getUrl(instanceInfo) + AuthController.CONTROLLER_PATH + "/invalidate/{}";
            restTemplate.delete(url, jwtToken);
        }

        return Boolean.TRUE;
    }

    /**
     * Checks if jwtToken is in the list of invalidated tokens.
     *
     * @param jwtToken token to check
     * @return true - token is invalidated, otherwise token is still valid
     */
    @Cacheable(value = CACHE_INVALIDATED_JWT_TOKENS, unless = "true", key = "#jwtToken", condition = "#jwtToken != null")
    public Boolean isInvalidated(String jwtToken) {
        return Boolean.FALSE;
    }

    /**
     * Method to translate original exception to internal one. It is used in case of parsing and verifying of JWT tokens.
     *
     * @param exception original exception
     * @return translated exception (better messaging and allow subsequent handling)
     */
    protected RuntimeException handleJwtParserException(RuntimeException exception) {
        if (exception instanceof ExpiredJwtException) {
            final ExpiredJwtException expiredJwtException = (ExpiredJwtException) exception;
            log.debug("Token with id '{}' for user '{}' is expired.", expiredJwtException.getClaims().getId(), expiredJwtException.getClaims().getSubject());
            return new TokenExpireException("Token is expired.");
        }
        if (exception instanceof JwtException) {
            log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
            return new TokenNotValidException("Token is not valid.");
        }

        log.debug(TOKEN_IS_NOT_VALID_DUE_TO, exception.getMessage());
        return new TokenNotValidException("An internal error occurred while validating the token therefor the token is no longer valid.");
    }

    private Claims validateAndParseLocalJwtToken(String jwtToken) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(jwtSecurityInitializer.getJwtPublicKey())
                .build()
                .parseClaimsJws(jwtToken)
                .getBody();
        } catch (RuntimeException exception) {
            throw handleJwtParserException(exception);
        }
    }

    /**
     * Method validate if jwtToken is valid or not. This method contains two types of verification:
     * - Zowe
     *   - it checks validity of signature
     *   - it checks if token is not expired
     *   - it checks if token was not removed (see logout)
     * - z/OSMF
     *   - it uses validation via REST directly in z/OSMF
     *
     * Method uses cache to speedup validation. In case of invalidating jwtToken in z/OSMF without Zowe, method
     * can return still true until cache will expired or be evicted.
     *
     * @param jwtToken token to verification
     * @return true if token is still valid, otherwise false
     */
    @Cacheable(value = CACHE_VALIDATION_JWT_TOKEN, key = "#jwtToken", condition = "#jwtToken != null")
    public TokenAuthentication validateJwtToken(String jwtToken) {
        QueryResponse queryResponse = parseJwtToken(jwtToken);

        switch (queryResponse.getSource()) {
            case ZOWE:
                validateAndParseLocalJwtToken(jwtToken);
                break;
            case ZOSMF:
                zosmfService.validate(JWT, jwtToken);
                break;
            default:
                throw new TokenNotValidException("Unknown token type.");
        }

        TokenAuthentication tokenAuthentication = new TokenAuthentication(queryResponse.getUserId(), jwtToken);
        // without a proxy cache aspect is not working, thus it is necessary get bean from application context
        final boolean authenticated = !meAsProxy.isInvalidated(jwtToken);
        tokenAuthentication.setAuthenticated(authenticated);

        return tokenAuthentication;
    }

    /**
     * Method constructs {@link TokenAuthentication} marked as valid. It also stores JWT token to the cache to
     * speed up next validation call.
     *
     * @param user     username to login
     * @param jwtToken token of user
     * @return authenticated {@link TokenAuthentication} using information about invalidating of token
     */
    @CachePut(value = "validationJwtToken", key = "#jwtToken", condition = "#jwtToken != null")
    public TokenAuthentication createTokenAuthentication(String user, String jwtToken) {
        final TokenAuthentication out = new TokenAuthentication(user, jwtToken);
        // without a proxy cache aspect is not working, thus it is necessary get bean from application context
        final boolean authenticated = !meAsProxy.isInvalidated(jwtToken);
        out.setAuthenticated(authenticated);
        return out;
    }

    /**
     * This method get all invalidated JWT token in the cache and distributes them to instance of Gateway with name
     * in argument toInstanceId. If instance cannot be find it return false. A notification can throw an runtime
     * exception. In all other cases all invalidated token are distributed and method returns true.
     *
     * @param toInstanceId instanceId of Gateway where invalidated JWT token should be sent
     * @return true if all token were sent, otherwise false
     */
    public boolean distributeInvalidate(String toInstanceId) {
        final Application application = discoveryClient.getApplication(CoreService.GATEWAY.getServiceId());
        if (application == null) return false;

        final InstanceInfo instanceInfo = application.getByInstanceId(toInstanceId);
        if (instanceInfo == null) return false;

        final String url = EurekaUtils.getUrl(instanceInfo) + AuthController.CONTROLLER_PATH + "/invalidate/{}";

        final Collection<String> invalidated = cacheUtils.getAllRecords(cacheManager, CACHE_INVALIDATED_JWT_TOKENS);
        for (final String invalidatedToken : invalidated) {
            restTemplate.delete(url, invalidatedToken);
        }

        return true;
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
        return meAsProxy.validateJwtToken(Optional.ofNullable(token).map(TokenAuthentication::getCredentials).orElse(null));
    }

    /**
     * This method removes the token signature. Each JWT token is concatenated of three parts (header, body, sign) joined
     * with ".". JWT library used for parsing contains also validation. A public key is needed for validation, but
     * we are also using JWT tokens from another application (z/OSMF) and we don't have it.
     *
     * @param jwtToken token to modify
     * @return jwt token without sign part
     */
    private String removeSign(String jwtToken) {
        if (jwtToken == null) return null;

        final int index = jwtToken.indexOf('.');
        final int index2 = jwtToken.indexOf('.', index + 1);
        if (index2 > 0) return jwtToken.substring(0, index2 + 1);

        return jwtToken;
    }

    /**
     * Parses the JWT token and return a {@link QueryResponse} object containing the domain, user id, type (Zowe / z/OSMF),
     * date of creation and date of expiration
     *
     * @param jwtToken the JWT token
     * @return the query response
     */
    public QueryResponse parseJwtToken(String jwtToken) {
        /*
         * Removes signature, because of z/OSMF we don't have key to verify certificate and
         * we just need to read claim. Verification is realized via REST call to z/OSMF.
         * JWT library doesn't parse signed key without verification.
         */
        final String withoutSign = removeSign(jwtToken);

        // parse to claims and construct QueryResponse
        try {
            Claims claims = Jwts.parserBuilder()
                .build()
                .parseClaimsJwt(withoutSign)
                .getBody();
            return new QueryResponse(
                claims.get(DOMAIN_CLAIM_NAME, String.class),
                claims.getSubject(),
                claims.getIssuedAt(),
                claims.getExpiration(),
                QueryResponse.Source.valueByIssuer(claims.getIssuer())
            );
        } catch (RuntimeException exception) {
            throw handleJwtParserException(exception);
        }
    }

    private Optional<String> getJwtTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(authConfigurationProperties.getCookieProperties().getCookieName()))
            .filter(cookie -> !cookie.getValue().isEmpty())
            .findFirst()
            .map(Cookie::getValue);
    }

    /**
     * Get the JWT token from the authorization header in the http request
     * <p>
     * Order:
     * 1. Cookie
     * 2. Authorization header
     *
     * @param request the http request
     * @return the JWT token
     */
    public Optional<String> getJwtTokenFromRequest(HttpServletRequest request) {
        Optional<String> fromCookie = getJwtTokenFromCookie(request);
        if (!fromCookie.isPresent()) {
            return extractJwtTokenFromAuthorizationHeader(request.getHeader(HttpHeaders.AUTHORIZATION));
        }
        return fromCookie;
    }

    /**
     * This method validates if JWT token is valid and if yes, then get claim from LTPA token.
     * For purpose, when is not needed validation, you can use method {@link #getLtpaToken(String)}
     *
     * @param jwtToken the JWT token
     * @return LTPA token extracted from JWT
     */
    public String getLtpaTokenWithValidation(String jwtToken) {
        return validateAndParseLocalJwtToken(jwtToken).get(LTPA_CLAIM_NAME, String.class);
    }

    /**
     * Get the LTPA token from the JWT token
     *
     * @param jwtToken the JWT token
     * @return the LTPA token
     * @throws TokenNotValidException if the JWT token is not valid
     */
    public String getLtpaToken(String jwtToken) {
        // remove sign to avoid validation of sign
        final String withoutSign = removeSign(jwtToken);

        // parse to claims and construct QueryResponse
        try {
            return Jwts.parserBuilder()
                .build()
                .parseClaimsJwt(withoutSign)
                .getBody()
                .get(LTPA_CLAIM_NAME, String.class);
        } catch (RuntimeException exception) {
            throw handleJwtParserException(exception);
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
