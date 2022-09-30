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

    @Value("${apiml.security.oAuth.clientId:}")
    private String clientId;

    @Value("${apiml.security.oAuth.clientSecret:}")
    private String clientSecret;

    @Value("${apiml.security.oAuth.validationUrl:}")
    private String validationUrl;

    private final RestTemplate restTemplate;

    @Override
    public boolean isValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            String credentials = clientId + ":" + clientSecret;
            byte[] base64encoded = Base64.getEncoder().encode(credentials.getBytes());
            headers.add("authorization", "Basic " + new String(base64encoded));
            headers.add("content-type", "application/x-www-form-urlencoded");
            ObjectMapper mapper = new JsonMapper();
            ResponseEntity<String> tokenInfoResponse = restTemplate.exchange(validationUrl + token, HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
            if (tokenInfoResponse.getStatusCode().is2xxSuccessful() &&
                tokenInfoResponse.getBody() != null &&
                !tokenInfoResponse.getBody().isEmpty()) {   //NOSONAR tests return null
                JsonNode json = mapper.readTree(tokenInfoResponse.getBody());
                return json.get("active").asBoolean();
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
