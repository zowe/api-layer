/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import io.jsonwebtoken.*;
import io.jsonwebtoken.lang.Collections;
import io.jsonwebtoken.security.UnsupportedKeyException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.token.OIDCProvider;

import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnExpression("'${apiml.security.oidc.enabled:false}' == 'true'")
public class OIDCTokenProvider implements OIDCProvider {

    private final LocatorAdapterKid keyLocator = new LocatorAdapterKid();


    @Value("${apiml.security.oidc.jwks.uri}")
    private List<String> jwksUri;

    @Value("${apiml.security.oidc.jwks.refreshInternalHours:1}")
    private int jwkRefreshInterval;

    @Qualifier("oidcJwtClock")
    private final Clock clock;
    private final DefaultResourceRetriever resourceRetriever;

    @Value("${apiml.security.oidc.userInfo.uri}")
    private String endpointUrl;

    private final CloseableHttpClient secureHttpClientWithKeystore;
    @Getter
    private final Map<String, PublicKey> publicKeys = new ConcurrentHashMap<>();
    @Getter
    private JWKSet jwkSet;

    @PostConstruct
    public void afterPropertiesSet() {
        this.fetchJWKSet();
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OIDC JWK Refresh"))
            .scheduleAtFixedRate(this::fetchJWKSet, jwkRefreshInterval, jwkRefreshInterval, TimeUnit.HOURS);
    }

    @Retryable
    void fetchJWKSet() {
        if (Collections.isEmpty(jwksUri)) {
            log.debug("OIDC JWK URI not provided, JWK refresh not performed");
            return;
        }
        log.debug("Refreshing JWK endpoints {}", jwksUri);

        try {
            publicKeys.clear();
            jwkSet = null;
            for (String url : jwksUri) {
                Resource resource = resourceRetriever.retrieveResource(new URL(url));
                jwkSet = JWKSet.parse(resource.getContent());
                publicKeys.putAll(processKeys(jwkSet));
            }
        } catch (IOException | ParseException | IllegalStateException e) {
            log.error("Error processing response from URI {} message: {}", jwksUri, e.getMessage());
        }
    }

    private Map<String, PublicKey> processKeys(JWKSet jwkKeys) {
        return jwkKeys.getKeys().stream()
            .filter(jwkKey -> {
                KeyUse keyUse = jwkKey.getKeyUse();
                KeyType keyType = jwkKey.getKeyType();
                return keyUse != null && keyType != null && "sig".equals(keyUse.getValue()) && "RSA".equals(keyType.getValue());
            })
            .collect(Collectors.toMap(JWK::getKeyID, jwkKey -> {
                try {
                    return jwkKey.toRSAKey().toRSAPublicKey();
                } catch (JOSEException e) {
                    log.debug("Problem with getting RSA Public key from JWK. ", e.getCause());
                    throw new IllegalStateException("Failed to parse public key", e);
                }
            }));
    }

    @Override
    public boolean isValid(String token) {
        try {
            log.debug("Validating the token with JWK: {}", jwksUri);
            if (Collections.isEmpty(jwksUri) || getClaims(token).isEmpty()) {
                return isValidExternal(token);
            }
            return true;
        } catch (MalformedJwtException jwte) {
            log.debug("Malformed JWT: {}", jwte.getMessage(), jwte.getCause());
            return false;
        } catch (JwtException jwte) {
            log.debug("JWK token validation failed with the exception {}", jwte.getMessage(), jwte.getCause());
            return isValidExternal(token);
        }
    }

    public boolean isValidExternal(String token) {
        try {
            if (StringUtils.isEmpty(endpointUrl)) {
                log.debug("JWT can't be validated externally because endpoint URL was not provided.");
                return false;
            }
            log.debug("Validating the token against URL: {}", endpointUrl);
            var httpGet = new HttpGet(endpointUrl);
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token);

            return secureHttpClientWithKeystore.execute(httpGet, response -> {
                final int responseCode = response.getCode();
                log.debug("Response code: {}", responseCode);
                return HttpStatus.valueOf(responseCode).is2xxSuccessful();
            });
        } catch (IOException e) {
            log.error("An error occurred during validation of OIDC token using userInfo URI {}: {}", endpointUrl, e.getMessage());
            return false;
        }
    }

    Claims getClaims(String token) {
        if (jwkSet == null || jwkSet.isEmpty()) {
            fetchJWKSet();
        }

        if (StringUtils.isBlank(token)) {
            throw new JwtException("Empty string provided instead of a token.");
        }

        return Jwts.parser()
            .clock(clock)
            .keyLocator(keyLocator)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    class LocatorAdapterKid extends LocatorAdapter<Key> {

        @Override
        protected Key locate(ProtectedHeader header) {
            if (jwkSet == null) {
                throw new JwtException("Could not validate the token due to missing public key.");
            }
            var kid = header.getKeyId();
            if (kid == null) {
                throw new UnsupportedKeyException("Token does not provide kid. It uses an unsupported type of signature.");
            }
            return Optional.ofNullable(jwkSet.getKeyByKeyId(header.getKeyId()))
                .map(key -> {
                    try {
                        return key.toRSAKey().toPublicKey();
                    } catch (JOSEException e) {
                        throw new JwtException("Could not validate the token due to either an invalid token or an invalid public key.", e);
                    }
                })
                .orElseThrow(() -> new UnsupportedKeyException("Key with id " + header.getKeyId() + " is null in JWK"));
        }

    }

}
