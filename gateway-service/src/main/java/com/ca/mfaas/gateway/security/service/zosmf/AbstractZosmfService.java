/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service.zosmf;

import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.error.ServiceNotAccessibleException;
import com.ca.mfaas.gateway.security.service.ZosmfService;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import com.ca.mfaas.util.EurekaUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public abstract class AbstractZosmfService implements ZosmfService {

    protected static final String ZOSMF_INFO_END_POINT = "/zosmf/info";
    protected static final String ZOSMF_AUTHENTICATE_END_POINT = "/zosmf/services/authenticate";
    protected static final String ZOSMF_CSRF_HEADER = "X-CSRF-ZOSMF-HEADER";
    protected static final String ZOSMF_DOMAIN = "zosmf_saf_realm";

    @InjectApimlLogger
    protected ApimlLogger apimlLog = ApimlLogger.empty();

    protected final AuthConfigurationProperties authConfigurationProperties;
    protected final DiscoveryClient discovery;
    protected final RestTemplate restTemplate;
    protected final ObjectMapper securityObjectMapper;

    public AbstractZosmfService(
        AuthConfigurationProperties authConfigurationProperties,
        DiscoveryClient discovery,
        RestTemplate restTemplate,
        ObjectMapper securityObjectMapper
    ) {
        this.authConfigurationProperties = authConfigurationProperties;
        this.discovery = discovery;
        this.restTemplate = restTemplate;
        this.securityObjectMapper = securityObjectMapper;
    }

    protected String getZosmfServiceId() {
        return authConfigurationProperties.validatedZosmfServiceId();
    }

    protected String getAuthenticationValue(Authentication authentication) {
        final String user = authentication.getPrincipal().toString();
        final String password = authentication.getCredentials().toString();

        final String credentials = user + ":" + password;
        final String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        return authorization;
    }

    /**
     * Return z/OSMF instance uri
     *
     * @param zosmf the z/OSMF service id
     * @return the uri
     */
    protected String getURI(String zosmf) {
        Supplier<ServiceNotAccessibleException> authenticationServiceExceptionSupplier = () -> {
            apimlLog.log("apiml.security.zosmfInstanceNotFound", zosmf);
            return new ServiceNotAccessibleException("z/OSMF instance not found or incorrectly configured.");
        };

        return Optional.ofNullable(discovery.getApplication(zosmf))
            .orElseThrow(authenticationServiceExceptionSupplier)
            .getInstances()
            .stream()
            .filter(Objects::nonNull)
            .findFirst()
            .map(zosmfInstance -> EurekaUtils.getUrl(zosmfInstance))
            .orElseThrow(authenticationServiceExceptionSupplier);
    }

    protected RuntimeException handleExceptionOnCall(String url, RuntimeException re) {
        if (re instanceof ResourceAccessException) {
            apimlLog.log("apiml.security.serviceUnavailable", url, re.getMessage());
            return new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
        }

        if (re instanceof RestClientException) {
            apimlLog.log("apiml.security.generic", re.getMessage(), url);
            return new AuthenticationServiceException("A failure occurred when authenticating.", re);
        }

        return re;
    }

    /**
     * Read the z/OSMF domain from the content in the response
     *
     * @param content the response body
     * @return the z/OSMF domain
     * @throws AuthenticationServiceException if the zosmf domain cannot be read
     */
    protected String readDomain(String content) {
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

    /**
     * Read the token with name cookieName from the cookies
     *
     * @param cookies the cookies
     * @return the token if is set in cookies, otherwise null
     */
    protected String readTokenFromCookie(List<String> cookies, String cookieName) {
        if (cookies == null) return null;

        return cookies.stream()
            .filter(x -> x.startsWith(cookieName + "="))
            .findFirst()
            .map(x -> {
                final int beginIndex = cookieName.length() + 1;
                final int endIndex = x.indexOf(';');
                return endIndex > 0 ? x.substring(beginIndex, endIndex) : x.substring(beginIndex);
            })
            .orElse(null);
    }

    protected AuthenticationResponse getAuthenticationResponse(ResponseEntity<String> responseEntity) {
        final List<String> cookies = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
        final Map<TokenType, String> tokens = new HashMap<>();
        for (final TokenType tokenType : TokenType.values()) {
            final String token = readTokenFromCookie(cookies, tokenType.getCookieName());
            if (token != null) tokens.put(tokenType, token);
        }
        final String domain = readDomain(responseEntity.getBody());
        return new AuthenticationResponse(domain, tokens);
    }

}
