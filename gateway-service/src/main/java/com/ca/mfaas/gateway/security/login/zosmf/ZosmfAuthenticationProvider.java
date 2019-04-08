/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.login.zosmf;

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Component
public class ZosmfAuthenticationProvider implements AuthenticationProvider {
    private static final String ZOSMF_END_POINT = "zosmf/info";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    private static final String ZOSMF_DOMAIN = "zosmf_saf_realm";

    private final SecurityConfigurationProperties securityConfigurationProperties;
    private final AuthenticationService authenticationService;
    private final DiscoveryClient discovery;
    private final ObjectMapper securityObjectMapper;
    private final RestTemplate restTemplate;

    public ZosmfAuthenticationProvider(SecurityConfigurationProperties securityConfigurationProperties,
                                       AuthenticationService authenticationService,
                                       DiscoveryClient discovery,
                                       ObjectMapper securityObjectMapper,
                                       RestTemplate restTemplate) {
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.discovery = discovery;
        this.authenticationService = authenticationService;
        this.securityObjectMapper = securityObjectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String zosmf = securityConfigurationProperties.validatedZosmfServiceId();
        String uri = getURI(zosmf);

        String user = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();

        String credentials = user + ":" + password;
        String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, authorization);
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                uri + ZOSMF_END_POINT,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);

            String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            String ltpaToken = readLtpaToken(cookie);
            String domain = readDomain(response.getBody());
            String jwtToken = authenticationService.createJwtToken(user, domain, ltpaToken);

            TokenAuthentication tokenAuthentication = new TokenAuthentication(user, jwtToken);
            tokenAuthentication.setAuthenticated(true);

            return tokenAuthentication;
        } catch (RestClientException e) {
            log.error("Can not access z/OSMF service. Uri '{}' returned: {}", uri, e.getMessage());
            throw new AuthenticationServiceException("A failure occurred when authenticating.", e);
        }
    }

    private String getURI(String zosmf) {
        Supplier<AuthenticationServiceException> authenticationServiceExceptionSupplier = () -> {
            log.error("z/OSMF instance '{}' not found or incorrectly configured.", zosmf);
            return new AuthenticationServiceException("z/OSMF instance not found or incorrectly configured.");
        };

        return Optional.ofNullable(discovery.getInstances(zosmf))
            .orElseThrow(authenticationServiceExceptionSupplier)
            .stream()
            .findFirst()
            .map(zosmfInstance -> zosmfInstance.getUri().toString())
            .orElseThrow(authenticationServiceExceptionSupplier);
    }

    private String readLtpaToken(String cookie) {
        if (cookie == null || cookie.isEmpty() || !cookie.contains("LtpaToken2")) {
            throw new BadCredentialsException("Username or password are invalid.");
        } else {
            int end = cookie.indexOf(';');
            return (end > 0) ? cookie.substring(0, end) : cookie;
        }
    }

    private String readDomain(String content) {
        ObjectNode zosmfNode;
        try {
            zosmfNode = securityObjectMapper.readValue(content, ObjectNode.class);
        } catch (IOException e) {
            log.error("Error parsing z/OSMF response.");
            throw new AuthenticationServiceException("z/OSMF domain cannot be read.");
        }

        if (zosmfNode != null && zosmfNode.has(ZOSMF_DOMAIN)) {
            return zosmfNode.get(ZOSMF_DOMAIN).asText();
        } else {
            log.error("z/OSMF response does not contain field '{}'.", ZOSMF_DOMAIN);
            throw new AuthenticationServiceException("z/OSMF domain cannot be read.");
        }
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth.equals(UsernamePasswordAuthenticationToken.class);
    }
}
