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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
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

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
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

    private Map<String, Key> jwks = new ConcurrentHashMap<>();

    @PostConstruct
    public void afterPropertiesSet() {
        this.fetchJwksUrls();
        Executors.newSingleThreadScheduledExecutor(r -> new Thread("OIDC JWK Refresh"))
            .scheduleAtFixedRate(this::fetchJwksUrls , jwkRefreshInterval, jwkRefreshInterval, TimeUnit.HOURS);
    }

    @Retryable
    void fetchJwksUrls() {
        if (StringUtils.isBlank(jwksUri)) {
            log.debug("OIDC JWK URI not provided, JWK refresh not performed");
            return;
        }
        log.debug("Refreshing JWK endpoints {}", jwksUri);
        HttpGet getRequest = new HttpGet(jwksUri);
        try {
            CloseableHttpResponse response = httpClient.execute(getRequest);
            final int statusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;
            final HttpEntity responseEntity = response.getEntity();
            String responseBody = "";
            if (responseEntity != null) {
                responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            }
            if (statusCode == HttpStatus.SC_OK && !responseBody.isEmpty()) {
                jwks.clear();
                JwkKeys jwkKeys = mapper.readValue(responseBody, JwkKeys.class);
                jwks.putAll(processKeys(jwkKeys));
            } else {
                log.error("Failed to obtain JWKs from URI {}. Unexpected response: {}, response text: {}", jwksUri, statusCode, responseBody);
            }
        } catch (IOException | IllegalStateException e) {
            log.error("Error processing response from URI {}", jwksUri, e.getMessage());
        }
    }

    private Map<String, Key> processKeys(JwkKeys jwkKeys) {
        return jwkKeys.getKeys().stream()
            .filter(jwkKey -> "sig".equals(jwkKey.getUse()))
            .filter(jwkKey -> "RSA".equals(jwkKey.getKty()))
            .collect(Collectors.toMap(JwkKeys.Key::getKid, jwkKey -> {
                BigInteger modulus = base64ToBigInteger(jwkKey.getN());
                BigInteger exponent = base64ToBigInteger(jwkKey.getE());
                RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    return keyFactory.generatePublic(rsaPublicKeySpec);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    throw new IllegalStateException("Failed to parse public key");
                }
           }));
    }

    private BigInteger base64ToBigInteger(String value) {
        return new BigInteger(1, Decoders.BASE64URL.decode(value));
    }

    @Override
    public boolean isValid(String token) {
        if (StringUtils.isBlank(token)) {
            log.debug("No token has been provided.");
            return false;
        }
        String kid = getKeyId(token);
        logger.log(MessageType.DEBUG, "Token signed by key {}", kid);
        return Optional.ofNullable(jwks.get(kid))
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
