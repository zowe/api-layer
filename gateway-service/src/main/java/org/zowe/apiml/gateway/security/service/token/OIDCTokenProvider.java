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
import io.jsonwebtoken.Jwts;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.common.token.OIDCProvider;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.zowe.apiml.gateway.security.service.JwtUtils.handleJwtParserException;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnProperty(value = "apiml.security.oidc.enabled", havingValue = "true")
public class OIDCTokenProvider implements OIDCProvider {

    @Value("${apiml.security.oidc.introspectUrl:}")
    String introspectUrl;

    @Value("${apiml.security.oidc.registry:}")
    String registry;

    @Value("${apiml.security.oidc.clientId:}")
    String clientId;

    @Value("${apiml.security.oidc.clientSecret:}")
    String clientSecret;

    @Autowired
    @Qualifier("secureHttpClientWithoutKeystore")
    @NonNull
    private final CloseableHttpClient httpClient;

    @Autowired
    private final ObjectMapper mapper;

    @Value("${apiml.security.oidc.jwks.uri}")
    private final String jwksUri;

    @Value("${apiml.security.oidc.jwks.refreshInternalHours:1}")
    private final Long jwkRefreshInterval;

    private final Map<String, JwkKeys> jwks = new ConcurrentHashMap<>();

    @PostConstruct
    public void afterPropertiesSet() {
        this.fetchJwksUrls();
        Executors.newSingleThreadScheduledExecutor(r -> new Thread("OIDC JWK Refresh"))
            .scheduleAtFixedRate(this::fetchJwksUrls , jwkRefreshInterval.longValue(), jwkRefreshInterval.longValue(), TimeUnit.HOURS);
    }

    void fetchJwksUrls() {
        if (StringUtils.isBlank(jwksUri)) {
            log.debug("OIDC JWK URI not provided, JWK refresh not performed");
            return;
        }
        log.debug("Refreshing JWK endpoints {}", jwksUri);
        HttpGet getRequest = new HttpGet(jwksUri + "?client_id=" + clientId);
        try {
            CloseableHttpResponse response = httpClient.execute(getRequest);
            final int statusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;
            final HttpEntity responseEntity = response.getEntity();
            String responseBody = "";
            if (responseEntity != null) {
                responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            }
            if (statusCode == HttpStatus.SC_OK && !responseBody.isEmpty()) {
                jwks.put(registry, mapper.readValue(responseBody, JwkKeys.class));
            } else {
                log.error("Failed to obtain JWKs from URI {}. Unexpected response: {}, response text: {}", jwksUri, statusCode, responseBody);
            }
        } catch (IOException e) {
            log.error("Error processing response from URI {}", jwksUri, e.getMessage());
        }
    }

    @Override
    public boolean isValid(String token) {
        if (StringUtils.isBlank(token)) {
            log.debug("No token has been provided.");
            return false;
        }
        try {
            Claims claims = null;

            Set<String> keySet = jwks.keySet();
            for(String key: keySet) {
                claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

                if (claims != null) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException exception) {
            throw handleJwtParserException(exception);
        }
    }
}
