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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApimlAccessTokenProvider implements AccessTokenProvider {

    private static final String CACHING_SERVICE_URI = "cachingservice/api/v1/cache";
    @Qualifier("secureHttpClientWithKeystore")
    private final CloseableHttpClient httpClient;
    private final GatewayClient gatewayClient;

    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public int invalidateToken(String token) {
        String tokenHash = DigestUtils.sha1Hex(token);
        return invalidateTokenByKey(tokenHash);
    }

    private int invalidateTokenByKey(String key) {
        HttpDelete revokeRequest = new HttpDelete(getCacheUrl() + "/revoke/" + key);
        try {
            CloseableHttpResponse resp = httpClient.execute(revokeRequest);
            log.debug("Revoked hash: " + key + " with status" + resp.getStatusLine().getStatusCode());
            return resp.getStatusLine().getStatusCode();
        } catch (IOException e) {
            log.error("Error while revoking token with hash: " + key);
            return 500;
        }
    }

    private String getCacheUrl(){
        return String.format("%s://%s/%s",gatewayClient.getGatewayConfigProperties().getScheme(),gatewayClient.getGatewayConfigProperties().getHostname(),CACHING_SERVICE_URI);
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

