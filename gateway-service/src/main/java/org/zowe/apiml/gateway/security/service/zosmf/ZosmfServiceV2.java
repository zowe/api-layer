/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.zosmf;

import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * This implementation is used for version z/OSMF which can support authentication
 * endpoint depends on z/OSMF's configuration).
 * This endpoint allows to generate new token, verify using token and
 * also deactivation of token. Token could be LTPA or JWT.
 *
 * Bean could be served via {@link ZosmfServiceFacade}
 */
@Service
@Order(1)
public class ZosmfServiceV2 extends AbstractZosmfService {

    public ZosmfServiceV2(AuthConfigurationProperties authConfigurationProperties, DiscoveryClient discovery,
            @Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore,
            ObjectMapper securityObjectMapper) {
        super(authConfigurationProperties, discovery, restTemplateWithoutKeystore, securityObjectMapper);
    }

    @Override
    public AuthenticationResponse authenticate(Authentication authentication) {
        String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;

        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, getAuthenticationValue(authentication));
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            final ResponseEntity<String> response = restTemplateWithoutKeystore.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers), String.class);
            return getAuthenticationResponse(response);
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(url, re);
        }
    }

    private boolean authenticationEndpointExists() {
        String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;

        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");

        try {
            restTemplateWithoutKeystore.exchange(url, HttpMethod.POST, new HttpEntity<>(null, headers), String.class);
        } catch (HttpClientErrorException hce) {
            if (hce.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                return true;
            }
        } catch (RuntimeException re) {
        }
        return false;
    }

    @Override
    public void validate(ZosmfService.TokenType type, String token) {
        final String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;

        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        headers.add(HttpHeaders.COOKIE, type.getCookieName() + "=" + token);

        try {
            ResponseEntity<String> re = restTemplateWithoutKeystore.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(null, headers), String.class);

            if (re.getStatusCode().is2xxSuccessful())
                return;
            if (re.getStatusCodeValue() == 401) {
                throw new TokenNotValidException("Token is not valid.");
            }
            apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, re.getStatusCodeValue());
            throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(url, re);
        }
    }

    @Override
    public void invalidate(ZosmfService.TokenType type, String token) {
        final String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;

        final HttpHeaders headers = new HttpHeaders();
        headers.add(ZOSMF_CSRF_HEADER, "");
        headers.add(HttpHeaders.COOKIE, type.getCookieName() + "=" + token);

        try {
            ResponseEntity<String> re = restTemplateWithoutKeystore.exchange(url, HttpMethod.DELETE,
                    new HttpEntity<>(null, headers), String.class);

            if (re.getStatusCode().is2xxSuccessful())
                return;
            apimlLog.log("org.zowe.apiml.security.serviceUnavailable", url, re.getStatusCodeValue());
            throw new ServiceNotAccessibleException("Could not get an access to z/OSMF service.");
        } catch (RuntimeException re) {
            throw handleExceptionOnCall(url, re);
        }
    }


    @Override
    public boolean isSupported(int version) {
        return (version >= 26) && authenticationEndpointExists();
    }

}
