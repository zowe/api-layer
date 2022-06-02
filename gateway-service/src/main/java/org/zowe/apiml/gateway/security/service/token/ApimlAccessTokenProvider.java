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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.cache.CachingServiceClient;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {

    private final CachingServiceClient cachingServiceClient;
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void invalidateToken(String token) throws CachingServiceClientException, JsonProcessingException {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedValue = encoder.encode(token);
        AccessTokenContainer container = new AccessTokenContainer();
        container.setTokenValue(hashedValue);
        String json = objectMapper.writeValueAsString(container);
        cachingServiceClient.appendList(new CachingServiceClient.KeyValue("invalidToken", json));
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

