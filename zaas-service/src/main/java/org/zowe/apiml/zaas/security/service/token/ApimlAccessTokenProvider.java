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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zowe.apiml.models.AccessTokenContainer;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.zaas.cache.CachingServiceClient;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {

    static final String INVALID_TOKENS_KEY = "invalidTokens";
    static final String INVALID_USERS_KEY = "invalidUsers";
    static final String INVALID_SCOPES_KEY = "invalidScopes";

    private final CachingServiceClient cachingServiceClient;
    private final AuthenticationService authenticationService;
    @Qualifier("oidcJwkMapper")
    private final ObjectMapper objectMapper;

    private byte[] salt;

    public void invalidateToken(String token) throws CachingServiceClientException, JsonProcessingException {
        String hashedValue = getHash(token);
        QueryResponse queryResponse = authenticationService.parseJwtWithSignature(token);
        AccessTokenContainer container = new AccessTokenContainer();
        container.setTokenValue(hashedValue);
        container.setIssuedAt(LocalDateTime.ofInstant(queryResponse.getCreation().toInstant(), ZoneId.systemDefault()));
        container.setExpiresAt(LocalDateTime.ofInstant(queryResponse.getExpiration().toInstant(), ZoneId.systemDefault()));

        String json = objectMapper.writeValueAsString(container);
        cachingServiceClient.appendList(INVALID_TOKENS_KEY, new CachingServiceClient.KeyValue(hashedValue, json));
    }

    public void invalidateAllTokensForUser(String userId, long timestamp) throws CachingServiceClientException {
        String hashedUserId = getHash(userId);
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        log.debug("hashedUserId {}, timestamp {}", hashedUserId, timestamp);
        cachingServiceClient.appendList(INVALID_USERS_KEY, new CachingServiceClient.KeyValue(hashedUserId, Long.toString(timestamp)));
    }

    public void invalidateAllTokensForService(String serviceId, long timestamp) throws CachingServiceClientException {
        String hashedServiceId = getHash(serviceId);
        if (timestamp == 0) {
            timestamp = System.currentTimeMillis();
        }
        log.debug("serviceIdHash {}, timestamp {}", hashedServiceId, timestamp);
        cachingServiceClient.appendList(INVALID_SCOPES_KEY, new CachingServiceClient.KeyValue(hashedServiceId, Long.toString(timestamp)));
    }

    public boolean isInvalidated(String token) throws CachingServiceClientException {
        QueryResponse parsedToken = authenticationService.parseJwtWithSignature(token);
        String hashedToken = getHash(token);
        String hashedUserId = getHash(parsedToken.getUserId());
        List<String> hashedServiceIds = parsedToken.getScopes().stream().map(this::getHash).toList();

        Map<String, Map<String, String>> cacheMap = cachingServiceClient.readAllMaps();
        if (cacheMap != null && !cacheMap.isEmpty()) {
            Map<String, String> invalidTokens = cacheMap.get(INVALID_TOKENS_KEY);
            Map<String, String> invalidUsers = cacheMap.get(INVALID_USERS_KEY);
            Map<String, String> invalidScopes = cacheMap.get(INVALID_SCOPES_KEY);
            Optional<Boolean> isInvalidated = checkInvalidToken(invalidTokens, hashedToken);
            if (isInvalidated.isEmpty()) {
                isInvalidated = checkRule(invalidUsers, hashedUserId, parsedToken);
            }
            for (String hashedServiceId : hashedServiceIds) {
                if (isInvalidated.isEmpty()) {
                    isInvalidated = checkRule(invalidScopes, hashedServiceId, parsedToken);
                } else {
                    break;
                }
            }
            if (isInvalidated.isPresent()) {
                return isInvalidated.get();
            }
        }
        return false;
    }

    private Optional<Boolean> checkInvalidToken(Map<String, String> invalidTokens, String tokenId) {
        if (invalidTokens != null && !invalidTokens.isEmpty() && invalidTokens.containsKey(tokenId)) {
            String s = invalidTokens.get(tokenId);
            try {
                AccessTokenContainer c = objectMapper.readValue(s, AccessTokenContainer.class);
                return Optional.of(c != null);
            } catch (JsonProcessingException e) {
                log.error("Not able to parse invalidToken json value.", e);
            }
        }
        return Optional.empty();
    }

    private Optional<Boolean> checkRule(Map<String, String> tokenRules, String ruleId, QueryResponse parsedToken) {
        if (tokenRules != null && !tokenRules.isEmpty() && tokenRules.containsKey(ruleId)) {
            String timestampStr = tokenRules.get(ruleId);
            try {
                long timestamp = Long.parseLong(timestampStr);
                var tokenTime = parsedToken.getCreation().getTime();
                boolean result = tokenTime <= timestamp;
                if (result) {
                    return Optional.of(true);
                }
            } catch (NumberFormatException e) {
                log.error("Not able to convert timestamp value to number.", e);
            }
        }
        return Optional.empty();
    }

    public void evictNonRelevantTokensAndRules() {
        cachingServiceClient.evictTokens(INVALID_TOKENS_KEY);
        cachingServiceClient.evictRules(INVALID_USERS_KEY);
        cachingServiceClient.evictRules(INVALID_SCOPES_KEY);
    }

    public String getHash(String token) throws CachingServiceClientException {
        return getSecurePassword(token, getSalt());
    }

    private String initializeSalt() throws CachingServiceClientException {
        String localSalt;
        try {
            CachingServiceClient.KeyValue keyValue = cachingServiceClient.read("salt");
            localSalt = keyValue.getValue();
        } catch (CachingServiceClientException e) {
            byte[] newSalt = generateSalt();
            storeSalt(newSalt);
            localSalt = new String(newSalt);
        }

        return localSalt;
    }

    public String getToken(String username, int expirationTime, Set<String> scopes) {
        int expiration = Math.min(expirationTime, 90);
        return authenticationService.createLongLivedJwtToken(username, expiration, scopes);
    }

    public boolean isValidForScopes(String jwtToken, String serviceId) {
        if (serviceId != null) {
            QueryResponse parsedToken = authenticationService.parseJwtWithSignature(jwtToken);
            if (parsedToken != null && parsedToken.getScopes() != null) {
                return parsedToken.getScopes().contains(serviceId.toLowerCase());
            }
        }
        return false;
    }

    public byte[] getSalt() throws CachingServiceClientException {
        if (this.salt != null) {
            return this.salt;
        }
        this.salt = initializeSalt().getBytes();
        return this.salt;
    }

    private void storeSalt(byte[] salt) throws CachingServiceClientException {
        cachingServiceClient.create(new CachingServiceClient.KeyValue("salt", new String(salt)));
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        try {
            SecureRandom.getInstanceStrong().nextBytes(salt);
            return salt;
        } catch (NoSuchAlgorithmException e) {
            throw new SecureTokenInitializationException(e);
        }
    }

    public static String getSecurePassword(String password, byte[] salt) {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(salt);
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not generate hash", e);
        }
        return generatedPassword;
    }
}

