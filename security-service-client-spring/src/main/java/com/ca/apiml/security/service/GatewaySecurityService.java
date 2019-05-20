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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Service
public class GatewaySecurityService {
    //TODO: Should not be hard-coded
    private static final String GATEWAY_PREFIX = "/api/v1/gateway/auth/login";

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
            gatewayConfigProperties.getHostname(), GATEWAY_PREFIX);

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

        } catch (RestClientException e) {
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
