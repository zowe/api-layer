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
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
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

    public GatewaySecurityService(GatewayConfigProperties gatewayConfigProperties,
                                  SecurityConfigurationProperties securityConfigurationProperties,
                                  RestTemplate restTemplate) {
        this.gatewayConfigProperties = gatewayConfigProperties;
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.restTemplate = restTemplate;
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
        }
        catch (HttpClientErrorException e) {
            //TODO: Should handle unsuccessful responses
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new BadCredentialsException("Username or password are invalid.");
            }

            log.error("Can not access Gateway service. Uri '{}' returned: {}", uri, e.getMessage());
            throw new AuthenticationServiceException("A failure occurred when authenticating.", e);
        }
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
