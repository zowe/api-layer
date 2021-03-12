/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.gateway.security.service.zosmf.ZosmfService.TokenType.LTPA;

@Primary
@Service
@Slf4j
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class ZosmfService extends AbstractZosmfService {

    private static final String CACHE_INVALIDATED_JWT_TOKENS = "invalidatedJwtTokens";

    /**
     * Enumeration of supported security tokens
     */
    @AllArgsConstructor
    @Getter
    public enum TokenType {

        JWT("jwtToken"),
        LTPA("LtpaToken2");

        private final String cookieName;

    }

    /**
     * Response of authentication, contains all data to next processing
     */
    @Data
    @AllArgsConstructor
    @RequiredArgsConstructor
    public static class AuthenticationResponse {

        private String domain;
        private final Map<TokenType, String> tokens;
    }

    /**
     * DTO with base information about z/OSMF (version and realm/domain)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZosmfInfo {


        @JsonProperty("zosmf_version")
        private int version;

        @JsonProperty("zosmf_full_version")
        private String fullVersion;

        @JsonProperty(ZOSMF_DOMAIN)
        private String safRealm;

    }

    private final ApplicationContext applicationContext;
    private final List<TokenValidationStrategy> tokenValidationStrategy;

    public ZosmfService(
        final AuthConfigurationProperties authConfigurationProperties,
        final DiscoveryClient discovery,
        final @Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore,
        final ObjectMapper securityObjectMapper,
        final ApplicationContext applicationContext,
        List<TokenValidationStrategy> tokenValidationStrategy
    ) {
        super(
            authConfigurationProperties,
            discovery,
            restTemplateWithoutKeystore,
            securityObjectMapper
        );
        this.applicationContext = applicationContext;
        this.tokenValidationStrategy = tokenValidationStrategy;
    }

    private ZosmfService meAsProxy;

    @PostConstruct
    public void afterPropertiesSet() {
        meAsProxy = applicationContext.getBean(ZosmfService.class);
    }


    @Retryable(value = {TokenNotValidException.class}, maxAttempts = 2, backoff = @Backoff(value = 1500))
    public AuthenticationResponse authenticate(Authentication authentication) {
        AuthenticationResponse authenticationResponse;
        if (loginEndpointExists()) {
            authenticationResponse = issueAuthenticationRequest(
                authentication,
                getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT,
                HttpMethod.POST);

            if (meAsProxy.isInvalidated(authenticationResponse.getTokens().get(JWT))) {
                invalidate(LTPA, authenticationResponse.getTokens().get(LTPA));
                throw new TokenNotValidException("Invalid token returned from zosmf");
            }
        } else {
            String zosmfInfoURIEndpoint = getURI(getZosmfServiceId()) + ZOSMF_INFO_END_POINT;
            authenticationResponse = issueAuthenticationRequest(
                authentication,
                zosmfInfoURIEndpoint,
                HttpMethod.GET);
            authenticationResponse.setDomain(meAsProxy.getZosmfRealm(zosmfInfoURIEndpoint));
        }
        return authenticationResponse;
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
     * @return String containing the zosmf realm/domain
     */
    @Cacheable("zosmfInfo")
    public String getZosmfRealm(String infoURIEndpoint) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            final ResponseEntity<ZosmfInfo> info = restTemplateWithoutKeystore.exchange(
                infoURIEndpoint,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ZosmfInfo.class
            );

            ZosmfInfo zosmfInfo = info.getBody();

            if (zosmfInfo == null || StringUtils.isEmpty(zosmfInfo.getSafRealm())) {
                apimlLog.log("apiml.security.zosmfDomainIsEmpty", ZOSMF_DOMAIN);
                throw new AuthenticationServiceException("z/OSMF domain cannot be read.");
            }

            return zosmfInfo.getSafRealm();
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(infoURIEndpoint, re);
        }
    }

    /**
     * Verify whether the service is actually accessible.
     *
     * @return true when it's possible to access the Info endpoint via GET.
     */
    public boolean isAccessible() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");

        String infoURIEndpoint = getURI(getZosmfServiceId()) + ZOSMF_INFO_END_POINT;
        log.debug("Verifying zOSMF accessibility on info endpoint: {}", infoURIEndpoint);

        try {
            final ResponseEntity<ZosmfInfo> info = restTemplateWithoutKeystore.exchange(
                infoURIEndpoint,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ZosmfInfo.class
            );

            return info.getStatusCode() == HttpStatus.OK;
        } catch (RestClientException ex) {
            log.debug("zOSMF isn't accessible on URI: {}", infoURIEndpoint);

            return false;
        }
    }

    /**
     * POST to provided url and return authentication response
     *
     * @param authentication
     * @param url            String containing auth endpoint to be used
     * @return AuthenticationResponse containing auth token, either LTPA or JWT
     */
    protected AuthenticationResponse issueAuthenticationRequest(Authentication authentication, String url, HttpMethod httpMethod) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getAuthenticationValue(authentication));
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            final ResponseEntity<String> response = restTemplateWithoutKeystore.exchange(
                url,
                httpMethod,
                new HttpEntity<>(null, headers), String.class);
            return getAuthenticationResponse(response);
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(url, re);
        }
    }

    /**
     * Check if call to ZOSMF_AUTHENTICATE_END_POINT resolves
     *
     * @param httpMethod HttpMethod to be checked for existence
     * @return boolean, containing true if endpoint resolves
     */
    @Cacheable(value = "zosmfAuthenticationEndpoint", key = "#httpMethod.name()")
    public boolean authenticationEndpointExists(HttpMethod httpMethod, HttpHeaders headers) {
        String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;

        try {
            restTemplateWithoutKeystore.exchange(url, httpMethod, new HttpEntity<>(null, headers), String.class);
        } catch (HttpClientErrorException hce) {
            if (HttpStatus.UNAUTHORIZED.equals(hce.getStatusCode())) {
                return true;
            } else if (HttpStatus.NOT_FOUND.equals(hce.getStatusCode())) {
                log.warn("The check of z/OSMF JWT authentication endpoint has failed, ensure APAR PH12143 " +
                    "(https://www.ibm.com/support/pages/apar/PH12143) fix has been applied. " +
                    "Using z/OSMF info endpoint as backup.");
                return false;
            } else {
                log.warn("z/OSMF authentication endpoint with HTTP method " + httpMethod.name() +
                    " has failed with status code: " + hce.getStatusCode(), hce);
                return false;
            }
        } catch (HttpServerErrorException serverError) {
            log.warn("z/OSMF internal error", serverError);
        }
        return false;
    }

    /**
     * Check if call to ZOSMF_JWT_END_POINT resolves
     *
     * @return true if endpoint resolves, otherwise false
     */
    @Cacheable(value = "zosmfJwtEndpoint")
    public boolean jwtEndpointExists(HttpHeaders headers) {
        String url = getURI(getZosmfServiceId()) + authConfigurationProperties.getZosmfJwtEndpoint();

        try {
            restTemplateWithoutKeystore.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), String.class);
        } catch (HttpClientErrorException hce) {
            if (HttpStatus.UNAUTHORIZED.equals(hce.getStatusCode())) {
                return true;
            } else if (HttpStatus.NOT_FOUND.equals(hce.getStatusCode())) {
                log.warn("The check of z/OSMF JWT builder endpoint has failed");
                return false;
            } else {
                log.warn("z/OSMF JWT builder endpoint with HTTP method GET has failed with status code: "
                    + hce.getStatusCode(), hce);
                return false;
            }
        } catch (HttpServerErrorException serverError) {
            log.warn("z/OSMF internal error", serverError);
        }
        return false;
    }

    /**
     * Tries to call ZOSMF authentication endpoint with HTTP Post method
     *
     * @return true, if zosmf login endpoint is presented
     */
    public boolean loginEndpointExists() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        headers.add("Authorization", "Basic Og==");
        return meAsProxy.authenticationEndpointExists(HttpMethod.POST, headers);
    }

    /**
     * Tries to call ZOSMF authentication endpoint with HTTP Delete method
     *
     * @return true, if zosmf logout endpoint is presented
     */
    public boolean logoutEndpointExists() {
        return meAsProxy.authenticationEndpointExists(HttpMethod.DELETE, null);
    }

    /**
     * Tries to call ZOSMF JWT Builder endpoint
     *
     * @return true if endpoint exists, otherwise false
     */
    public boolean jwtBuilderEndpointExists() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        headers.add("Authorization", "Basic Og==");
        return meAsProxy.jwtEndpointExists(headers);
    }

    public boolean validate(String token) {
        log.debug("ZosmfService validating token: ....{}", StringUtils.right(token, 15));
        TokenValidationRequest request = new TokenValidationRequest(TokenType.JWT, token, getURI(getZosmfServiceId()), getEndpointMap());

        for (TokenValidationStrategy s : tokenValidationStrategy) {
            log.debug("Trying to validate token with strategy: {}", s.toString());
            try {
                s.validate(request);
                if (requestValidationIsDecided(request)) {
                    log.debug("Token validity has been successfully determined: {}", request.getAuthenticated());
                    break;
                }
            } catch (RuntimeException re) {
                log.debug("Exception during token validation:", re);
            }
        }
        log.debug("Token validation strategies exhausted, final validation status: {}", request.getAuthenticated());
        return requestIsAuthenticated(request);
    }

    private boolean requestIsAuthenticated(TokenValidationRequest request) {
        return TokenValidationRequest.STATUS.AUTHENTICATED.equals(request.getAuthenticated());
    }

    private boolean requestValidationIsDecided(TokenValidationRequest request) {
        return !TokenValidationRequest.STATUS.UNKNOWN.equals(request.getAuthenticated());
    }

    public Map<String, Boolean> getEndpointMap() {
        Map<String, Boolean> endpointMap = new HashMap<>();

        endpointMap.put(getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT, loginEndpointExists());

        return endpointMap;
    }

    public void invalidate(TokenType type, String token) {
        if (logoutEndpointExists()) {
            final String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;

            final HttpHeaders headers = new HttpHeaders();
            headers.add(ZOSMF_CSRF_HEADER, "");
            headers.add(HttpHeaders.COOKIE, type.getCookieName() + "=" + token);

            try {
                ResponseEntity<String> re = restTemplateWithoutKeystore.exchange(url, HttpMethod.DELETE,
                    new HttpEntity<>(null, headers), String.class);

                if (re.getStatusCode().is2xxSuccessful())
                    return;
                apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, re.getStatusCodeValue());
                throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
            } catch (RuntimeException re) {
                throw handleExceptionOnCall(url, re);
            }
        }
        log.warn("The request to invalidate an auth token was unsuccessful, z/OSMF invalidate endpoint not available");
    }

    /**
     * Method reads authentication values from answer of REST call. It read all supported tokens, which are returned
     * from z/OSMF.
     *
     * @param responseEntity answer of REST call
     * @return AuthenticationResponse with all supported tokens from responseEntity
     */
    protected ZosmfService.AuthenticationResponse getAuthenticationResponse(ResponseEntity<String> responseEntity) {
        final List<String> cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
        final EnumMap<TokenType, String> tokens = new EnumMap<>(ZosmfService.TokenType.class);
        if (cookies != null) {
            for (final ZosmfService.TokenType tokenType : ZosmfService.TokenType.values()) {
                final String token = readTokenFromCookie(cookies, tokenType.getCookieName());
                if (token != null) tokens.put(tokenType, token);
            }
        }
        return new ZosmfService.AuthenticationResponse(tokens);
    }

    public JWKSet getPublicKeys() {
        final String url = getURI(getZosmfServiceId()) + authConfigurationProperties.getZosmfJwtEndpoint();

        try {
            final String json = restTemplateWithoutKeystore.getForObject(url, String.class);
            return JWKSet.parse(json);
        } catch (ParseException pe) {
            log.debug("Invalid format of public keys from z/OSMF", pe);
        } catch (HttpClientErrorException.NotFound nf) {
            log.debug("Cannot get public keys from z/OSMF", nf);
        }
        return new JWKSet();
    }
}
