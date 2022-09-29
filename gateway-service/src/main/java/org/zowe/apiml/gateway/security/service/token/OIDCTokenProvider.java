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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.token.OIDCProvider;

import java.util.Base64;

@RequiredArgsConstructor
@Service
@Slf4j
public class OIDCTokenProvider implements OIDCProvider {

    @Value("${apiml.service.security.oidcToken.okta.clientId}")
    private String clientId;

    @Value("${apiml.service.security.oidcToken.okta.clientSecret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    static final String TOKEN_ENDPOINT = "https://dev-95727686.okta.com:443/oauth2/default/v1/token?grant_type=client_credentials&scope=customScope";
    static final String INTROSPECT_ENDPOINT = "https://dev-95727686.okta.com:443/oauth2/default/v1/introspect?token=";

    @Override
    public boolean isValid() {
        try {
            HttpHeaders headers = new HttpHeaders();
            String creds = clientId + ":" + clientSecret;
            byte[] base64encoded = Base64.getEncoder().encode(creds.getBytes());
            headers.add("authorization", "Basic " + new String(base64encoded));
            headers.add("content-type", "application/x-www-form-urlencoded");
            ResponseEntity<String> accessTokenResponse = restTemplate.exchange(TOKEN_ENDPOINT, HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
            if (accessTokenResponse.getStatusCode().is2xxSuccessful()) {
                ObjectMapper mapper = new JsonMapper();
                String tokenBody = accessTokenResponse.getBody();
                if (tokenBody != null && !tokenBody.isEmpty()) {
                    JsonNode json = mapper.readTree(tokenBody);
                    String tokenValue = json.get("access_token").asText();
                    ResponseEntity<String> tokenInfoResponse = restTemplate.exchange(INTROSPECT_ENDPOINT + tokenValue, HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
                    if (tokenInfoResponse.getStatusCode().is2xxSuccessful()) {
                        if (tokenInfoResponse.getBody() != null && !tokenInfoResponse.getBody().isEmpty()) {
                            json = mapper.readTree(tokenInfoResponse.getBody());
                            return json.get("active").asBoolean();
                        }
                    }
                }
            }
        } catch (RestClientException e) {
            return false;
        } catch (JsonProcessingException e) {
            log.debug("Could not convert Swagger to JSON", e);
            return false;
        }
        return false;
    }

}
