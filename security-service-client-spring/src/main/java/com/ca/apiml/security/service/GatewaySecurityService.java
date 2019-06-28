/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.service;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.error.ErrorType;
import com.ca.apiml.security.handler.RestResponseHandler;
import com.ca.apiml.security.token.QueryResponse;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
public class GatewaySecurityService {

    private final GatewayConfigProperties gatewayConfigProperties;
    private final SecurityConfigurationProperties securityConfigurationProperties;
    private final RestTemplate restTemplate;
    private final RestResponseHandler responseHandler;

    public GatewaySecurityService(GatewayConfigProperties gatewayConfigProperties,
                                  SecurityConfigurationProperties securityConfigurationProperties,
                                  RestTemplate restTemplate,
                                  RestResponseHandler responseHandler) {
        this.gatewayConfigProperties = gatewayConfigProperties;
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.restTemplate = restTemplate;
        this.responseHandler = responseHandler;
    }

    public Optional<String> login(String username, String password) {

        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), securityConfigurationProperties.getGatewayLoginEndpoint());

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode loginRequest = mapper.createObjectNode();
        loginRequest.put("username", username);
        loginRequest.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uri,
                HttpMethod.POST,
                new HttpEntity<>(loginRequest, headers),
                String.class);

            return extractToken(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        } catch (HttpClientErrorException e) {
            responseHandler.handleBadResponse(e, ErrorType.BAD_CREDENTIALS,
                "Can not access Gateway service. Uri '{}' returned: {}", uri, e.getMessage());
        }
        return Optional.empty();
    }

    public QueryResponse query(String token) {

        String uri = String.format("%s://%s%s", gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(), securityConfigurationProperties.getGatewayQueryEndpoint());
        String cookie = String.format("%s=%s", securityConfigurationProperties.getCookieProperties().getCookieName(), token);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        try {
            ResponseEntity<QueryResponse> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                QueryResponse.class);

            return response.getBody();
        } catch (HttpClientErrorException e) {
            responseHandler.handleBadResponse(e, ErrorType.TOKEN_NOT_VALID,
                "Can not access Gateway service. Uri '{}' returned: {}", uri, e.getMessage());
        }
        return null;
    }

    private Optional<String> extractToken(String cookies) {
        String cookieName = securityConfigurationProperties.getCookieProperties().getCookieName();

        if (cookies == null || cookies.isEmpty() || !cookies.contains(cookieName)) {
            return Optional.empty();
        } else {
            int end = cookies.indexOf(';');
            String cookie = (end > 0) ? cookies.substring(0, end) : cookies;
            return Optional.of(cookie.replace(cookieName + "=", ""));
        }
    }
}
