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
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Service
public class ZosmfServiceV1 extends AbstractZosmfService {

    public ZosmfServiceV1(
        AuthConfigurationProperties authConfigurationProperties,
        DiscoveryClient discovery,
        RestTemplate restTemplate,
        ObjectMapper securityObjectMapper
    ) {
        super(authConfigurationProperties, discovery, restTemplate, securityObjectMapper);
    }

    @Override
    public AuthenticationResponse authenticate(Authentication authentication) {
        final String url = getURI(getZosmfServiceId()) + ZOSMF_INFO_END_POINT;

        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getAuthenticationValue(authentication));
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            final ResponseEntity<String> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
            return getAuthenticationResponse(responseEntity);
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(url, re);
        }
    }

    @Override
    public void validate(TokenType type, String token) {
        final String url = getURI(getZosmfServiceId()) + ZOSMF_INFO_END_POINT;

        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        headers.add(HttpHeaders.COOKIE, type.getCookieName() + "=" + token);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);

            if (response.getStatusCode().is2xxSuccessful()) return;
            if (response.getStatusCodeValue() == 401) {
                throw new TokenNotValidException("Token is not valid.");
            }
            apimlLog.log("apiml.security.serviceUnavailable", url, response.getStatusCodeValue());
            throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(url, re);
        }
    }

    @Override
    public void invalidate(TokenType type, String token) {
        // not supported by z/OSMF
    }

    @Override
    public boolean matchesVersion(int version) {
        return version <= 25;
    }

}
