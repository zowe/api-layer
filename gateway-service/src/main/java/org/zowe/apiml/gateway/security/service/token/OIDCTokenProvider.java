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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.util.UrlUtils;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
                jwks.put(registry, mapper.readValue(responseBody, JwkKeys.class));
            } else {
                log.error("Failed to validate the OIDC access token. Unexpected response: {}", statusCode); // TODO documented message?
            }
        } catch (IOException e) {
            log.error("", e.getMessage()); // TODO documented error message
        }
    }

    @Override
    public boolean isValid(String token) {
        // Should validate againts the provider
        OIDCTokenClaims claims = introspect(token);
        if (claims != null) {
            return claims.getActive();
        }
        return false;
    }

    private OIDCTokenClaims introspect(String token) {
        if (StringUtils.isBlank(token)) {
            log.debug("No token has been provided.");
            return null;
        }
        if (StringUtils.isBlank(introspectUrl) || !UrlUtils.isValidUrl(introspectUrl)) {
            log.warn("Missing or invalid introspectUrl configuration. Cannot proceed with token validation.");
            return null;
        }
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(clientSecret)) {
            log.warn("Missing clientId or clientSecret configuration. Cannot proceed with token validation.");
            return null;
        }
        HttpPost post = new HttpPost(introspectUrl);
        List<NameValuePair> bodyParams = new ArrayList<>();
        bodyParams.add(new BasicNameValuePair("token", token));
        bodyParams.add(new BasicNameValuePair("token_type_hint", "access_token"));
        post.setEntity(new UrlEncodedFormEntity(bodyParams, StandardCharsets.UTF_8));

        String credentials = clientId + ":" + clientSecret;
        byte[] base64encoded = Base64.getEncoder().encode(credentials.getBytes());
        final String headerValue = "Basic " + new String(base64encoded);
        post.setHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, headerValue));
        post.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE));
        post.setHeader(new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        try {
            CloseableHttpResponse response = httpClient.execute(post);
            final int statusCode = response.getStatusLine() != null ? response.getStatusLine().getStatusCode() : 0;
            final HttpEntity responseEntity = response.getEntity();
            String responseBody = "";
            if (responseEntity != null) {
                responseBody = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            }
            if (statusCode == HttpStatus.SC_OK && !responseBody.isEmpty()) {
                return mapper.readValue(responseBody, OIDCTokenClaims.class);
            } else {
                log.error("Failed to validate the OIDC access token. Unexpected response: {}", statusCode);
                return null;
            }
        } catch (IOException e) {
            log.error("Failed to validate the OIDC access token. ", e);
        }
        return null;
    }

}
