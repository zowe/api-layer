/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.token;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.text.ParseException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnProperty(value = "apiml.security.oidc.enabled", havingValue = "true")
public class OIDCTokenProvider implements OIDCProvider {

    @InjectApimlLogger
    protected final ApimlLogger logger = ApimlLogger.empty();

    @Value("${apiml.security.oidc.registry:}")
    String registry;

    @Value("${apiml.security.oidc.clientId:}")
    String clientId;

    @Value("${apiml.security.oidc.clientSecret:}")
    String clientSecret;

    @Value("${apiml.security.oidc.jwks.uri}")
    private String jwksUri;

    @Value("${apiml.security.oidc.jwks.refreshInternalHours:1}")
    private int jwkRefreshInterval;

    @Qualifier("secureHttpClientWithoutKeystore")
    @NonNull
    private final CloseableHttpClient httpClient;

    @Qualifier("oidcJwtClock")
    private final Clock clock;

    @Qualifier("oidcJwkMapper")
    private final ObjectMapper mapper;

    @Getter
    private Map<String, Key> publicKeys = new ConcurrentHashMap<>();
    @Getter
    private JWKSet jwkSet;

    @PostConstruct
    public void afterPropertiesSet() {
        this.fetchJWKSet();
        Executors.newSingleThreadScheduledExecutor(r -> new Thread("OIDC JWK Refresh"))
            .scheduleAtFixedRate(this::fetchJWKSet, jwkRefreshInterval, jwkRefreshInterval, TimeUnit.HOURS);
    }

    @Retryable
    void fetchJWKSet() {
        if (StringUtils.isBlank(jwksUri)) {
            log.debug("OIDC JWK URI not provided, JWK refresh not performed");
            return;
        }
        log.debug("Refreshing JWK endpoints {}", jwksUri);

        try {
            publicKeys.clear();
            jwkSet = null;
            jwkSet = JWKSet.load(new URL(jwksUri));
            publicKeys.putAll(processKeys(jwkSet));
        } catch (IOException | ParseException | IllegalStateException e) {
            log.error("Error processing response from URI {} message: {}", jwksUri, e.getMessage());
        }
    }


    private Map<String, Key> processKeys(JWKSet jwkKeys) {
        return jwkKeys.getKeys().stream()
            .filter(jwkKey -> "sig".equals(jwkKey.getKeyUse().getValue()) || "RSA".equals(jwkKey.getKeyType().getValue()))
            .collect(Collectors.toMap(JWK::getKeyID, jwkKey -> {
                try {
                    return jwkKey.toRSAKey().toRSAPublicKey();
                } catch (JOSEException e) {
                    log.error("Error", e.getCause());
                    throw new IllegalStateException("Failed to parse public key", e);
                }
            }));
    }

    @Override
    public boolean isValid(String token) {
        if (StringUtils.isBlank(token)) {
            log.debug("No token has been provided.");
            return false;
        }
        String kid = getKeyId(token);
        logger.log(MessageType.DEBUG, "Token signed by key {}", kid);
        return Optional.ofNullable(publicKeys.get(kid))
            .map(key -> validate(token, key))
            .map(claims -> claims != null && !claims.isEmpty())
            .orElse(false);
    }

    private String getKeyId(String token) {
        try {
            return String.valueOf(Jwts.parserBuilder()
                .setClock(clock)
                .build()
                .parseClaimsJwt(token.substring(0, token.lastIndexOf('.') + 1))
                .getHeader()
                .get("kid"));
        } catch (JwtException e) {
            log.error("OIDC Token is not valid: {}", e.getMessage());
            return "";
        }
    }

    private Claims validate(String token, Key key) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(key)
                .setClock(clock)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (TokenNotValidException | JwtException e) {
            log.debug("OIDC Token is not valid: {}", e.getMessage());
            return null; // NOSONAR
        }
    }

}
