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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.DiscoveryClient;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import java.text.ParseException;

@Primary
@Service
@Slf4j
public class ZosmfService extends AbstractZosmfService {
    //TODO:: Do we still need an abstract parent? only one implementation now...

    private static final String PUBLIC_JWK_ENDPOINT = "/jwt/ibm/api/zOSMFBuilder/jwk";

    public ZosmfService(
        final AuthConfigurationProperties authConfigurationProperties,
        final DiscoveryClient discovery,
        final @Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore,
        final ObjectMapper securityObjectMapper,
        final ApplicationContext applicationContext
    ) {
        super(
            authConfigurationProperties,
            discovery,
            restTemplateWithoutKeystore,
            securityObjectMapper
        );
    }

    @Override
    public AuthenticationResponse authenticate(Authentication authentication) {
        String url = getURI(getZosmfServiceId()) + ZOSMF_AUTHENTICATE_END_POINT;
        if (authenticationEndpointExists()) {
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
        throw handleExceptionOnCall(url, new RuntimeException("Create new zosmf PTF missing exception")); //TODO:: Create Exception class
    }

    /**
     * TODO:: MAKE this cachable?
     * @return
     */
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
            else if (hce.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return false;
            }
            log.warn("The check of z/OSMF JWT authentication endpoint has failed with exception", hce);
        } catch (RuntimeException re) {
            log.warn("The check of z/OSMF JWT authentication endpoint has failed with exception", re);
        }
        return false;
    }

    @Override
    public void validate(TokenType type, String token) {
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
    public void invalidate(TokenType type, String token) {
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

    public JWKSet getPublicKeys() {
        final String url = getURI(getZosmfServiceId()) + PUBLIC_JWK_ENDPOINT;

        try {
            final String json = restTemplateWithoutKeystore.getForObject(url, String.class);
            return JWKSet.parse(json);
        } catch (ParseException pe) {
            log.debug("Invalid format of public keys from z/OSMF", pe);
        } catch (HttpClientErrorException.NotFound nf) {
            log.debug("Cannot get public keys from z/OSMF", nf);
        }
        return new JWKSet();
    }
}
