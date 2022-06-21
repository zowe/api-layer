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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.cache.CachingServiceClient;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.zowe.apiml.gateway.security.service.JwtUtils.getJwtClaims;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {


    private final CachingServiceClient cachingServiceClient;
    private final AuthenticationService authenticationService;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private byte[] salt;

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void invalidateToken(String token) throws CachingServiceClientException, JsonProcessingException {
        String hashedValue = getHash(token);
        QueryResponse queryResponse = authenticationService.parseJwtToken(token);
        AccessTokenContainer container = new AccessTokenContainer();
        container.setTokenValue(hashedValue);
        container.setIssuedAt(LocalDateTime.ofInstant(queryResponse.getCreation().toInstant(), ZoneId.systemDefault()));
        container.setExpiresAt(LocalDateTime.ofInstant(queryResponse.getExpiration().toInstant(), ZoneId.systemDefault()));

        String json = objectMapper.writeValueAsString(container);
        cachingServiceClient.appendList(new CachingServiceClient.KeyValue(hashedValue, json));
    }

    public boolean isInvalidated(String token) throws CachingServiceClientException {
        String hash = getHash(token);
        Map<String, String> map = cachingServiceClient.readInvalidatedTokens();
        if (map != null && !map.isEmpty() && map.containsKey(hash)) {
            String s = map.get(hash);
            try {
                AccessTokenContainer c = objectMapper.readValue(s, AccessTokenContainer.class);
                return c != null;
            } catch (JsonProcessingException e) {
                log.error("Not able to parse json", e);
            }
        }
        return false;
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
       expirationTime = Math.min(expirationTime, 90);
       if (expirationTime <= 0) {
           expirationTime = 90;
       }
       return authenticationService.createLongLivedJwtToken(username, expirationTime, scopes);
    }

    public boolean isValidForScopes(String jwtToken, String serviceId) {
        if(serviceId != null) {
            Claims jwtClaims = getJwtClaims(jwtToken);
            if (jwtClaims != null) {
                Object scopesObject = jwtClaims.get("scopes");
                if(scopesObject instanceof List<?>) {
                    List<String>scopes = (List<String>) scopesObject;
                    return scopes.contains(serviceId);
                }
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
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
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

    @Data
    @AllArgsConstructor
    public static class AccessTokenContainer {

        public AccessTokenContainer() {
            // no args constructor
        }

        private String userId;
        private String tokenValue;
        private LocalDateTime issuedAt;
        private LocalDateTime expiresAt;
        private Set<String> scopes;
        private String tokenProvider;

    }
}

