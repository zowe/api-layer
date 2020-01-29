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

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.error.ServiceNotAccessibleException;
import com.ca.apiml.security.common.token.TokenAuthentication;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Authentication provider that verifies credentials against z/OSMF service
 */
@Component
public class ZosmfAuthenticationProvider implements AuthenticationProvider {
    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private static final String ZOSMF_END_POINT = "zosmf/info";
    private static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    private static final String ZOSMF_DOMAIN = "zosmf_saf_realm";

    private final AuthConfigurationProperties authConfigurationProperties;
    private final AuthenticationService authenticationService;
    private final DiscoveryClient discovery;
    private final ObjectMapper securityObjectMapper;
    private final RestTemplate restTemplate;

    public ZosmfAuthenticationProvider(AuthConfigurationProperties authConfigurationProperties,
                                       AuthenticationService authenticationService,
                                       DiscoveryClient discovery,
                                       ObjectMapper securityObjectMapper,
                                       RestTemplate restTemplateWithoutKeystore) {
        this.authConfigurationProperties = authConfigurationProperties;
        this.discovery = discovery;
        this.authenticationService = authenticationService;
        this.securityObjectMapper = securityObjectMapper;
        this.restTemplate = restTemplateWithoutKeystore;
    }

    /**
     * Authenticate the credentials with the z/OSMF service
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        String zosmf = authConfigurationProperties.validatedZosmfServiceId();
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

            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);

            String ltpaToken = readLtpaToken(cookies);
            String domain = readDomain(response.getBody());
            String jwtToken = authenticationService.createJwtToken(user, domain, ltpaToken);

            TokenAuthentication tokenAuthentication = new TokenAuthentication(user, jwtToken);
            tokenAuthentication.setAuthenticated(true);

            return tokenAuthentication;
        } catch (ResourceAccessException e) {
            apimlLog.log("apiml.security.serviceUnavailable", uri, e.getMessage());
            throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
        } catch (RestClientException e) {
            apimlLog.log("apiml.security.generic", e.getMessage(), uri);
            throw new AuthenticationServiceException("A failure occurred when authenticating.", e);
        }
    }

    /**
     * Return z/OSMF instance uri
     *
     * @param zosmf the z/OSMF service id
     * @return the uri
     */
    private String getURI(String zosmf) {
        Supplier<ServiceNotAccessibleException> authenticationServiceExceptionSupplier = () -> {
            apimlLog.log("apiml.security.zosmfInstanceNotFound", zosmf);
            return new ServiceNotAccessibleException("z/OSMF instance not found or incorrectly configured.");
        };

        return Optional.ofNullable(discovery.getInstances(zosmf))
            .orElseThrow(authenticationServiceExceptionSupplier)
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .map(zosmfInstance -> zosmfInstance.getUri().toString())
            .orElseThrow(authenticationServiceExceptionSupplier);
    }

    /**
     * Read the LTPA token from the cookies
     *
     * @param cookies the cookies
     * @return the LPTA token
     * @throws BadCredentialsException if the cookie does not contain valid LTPA token
     */
    private String readLtpaToken(List<String> cookies) {
        Supplier<BadCredentialsException> exceptionSupplier = () -> new BadCredentialsException("Username or password are invalid.");

        return Optional.ofNullable(cookies)
            .orElseThrow(exceptionSupplier)
            .stream()
            .filter(cookie -> cookie != null && cookie.contains("LtpaToken2"))
            .map(this::convertCookieToLtpaToken)
            .findFirst()
            .orElseThrow(exceptionSupplier);
    }

    private String convertCookieToLtpaToken(String content) {
        int end = content.indexOf(';');
        return (end > 0) ? content.substring(0, end) : content;
    }

    /**
     * Read the z/OSMF domain from the content in the response
     *
     * @param content the response body
     * @return the z/OSMF domain
     * @throws AuthenticationServiceException if the zosmf domain cannot be read
     */
    private String readDomain(String content) {
        try {
            ObjectNode zosmfNode = securityObjectMapper.readValue(content, ObjectNode.class);

            return Optional.ofNullable(zosmfNode)
                .filter(zn -> zn.has(ZOSMF_DOMAIN))
                .map(zn -> zn.get(ZOSMF_DOMAIN).asText())
                .orElseThrow(() -> {
                    apimlLog.log("apiml.security.zosmfDomainIsEmpty", ZOSMF_DOMAIN);
                    return new AuthenticationServiceException("z/OSMF domain cannot be read.");
                });
        } catch (IOException e) {
            apimlLog.log("apiml.security.errorParsingZosmfResponse", e.getMessage());
            throw new AuthenticationServiceException("z/OSMF domain cannot be read.");
        }
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth.equals(UsernamePasswordAuthenticationToken.class);
    }
}
