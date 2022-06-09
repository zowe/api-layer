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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.cache.CachingServiceClient;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {

    public static final Argon2PasswordEncoder ENCODER = new Argon2PasswordEncoder();

    private final CachingServiceClient cachingServiceClient;
    private final AuthenticationService authenticationService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        cachingServiceClient.appendList(new CachingServiceClient.KeyValue(token, json));
    }

    public boolean isInvalidated(String token) throws CachingServiceClientException {
        Map<String,String> map = cachingServiceClient.readList(token);
       if(map != null && !map.isEmpty()) {

           String s = map.get(token);
           try {
               AccessTokenContainer c = objectMapper.readValue(s,AccessTokenContainer.class);
               return true;
           } catch (JsonProcessingException e) {
               e.printStackTrace();
           }
       }
//        if (invalidatedTokenList != null && invalidatedTokenList.length > 0) {
//            for (AccessTokenContainer invalidatedToken : invalidatedTokenList) {
//                if (validateToken(token, invalidatedToken.getTokenValue())) {
//                    return true;
//                }
//            }
//        }
        return false;
    }

    public String getHash(String token) {
        return ENCODER.encode(token);
    }

    private boolean validateToken(String token, String hash) {
        return ENCODER.matches(token, hash);
    }

    @Data
    @AllArgsConstructor
    public static class AccessTokenContainer {

        public AccessTokenContainer() {
        }

        private String userId;
        private String tokenValue;
        private LocalDateTime issuedAt;
        private LocalDateTime expiresAt;
        private Set<String> scopes;
        private String tokenProvider;

    }
}

