/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service.saf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Authentication provider implementation for the SafIdt Tokens.
 */
@RequiredArgsConstructor
@Slf4j
public class SafRestAuthenticationService implements SafIdtProvider {
    private final RestTemplate restTemplate;

    @Value("${apiml.security.saf.urls.authenticate}")
    private String authenticationUrl;
    @Value("${apiml.security.saf.urls.verify}")
    private String verifyUrl;

    @Override
    public Optional<String> generate(String username) {
        // TODO: Use the JWT token
        // TODO: Build properly the

        try {
            ResponseEntity<String> re = restTemplate.exchange(authenticationUrl, HttpMethod.POST,
                new HttpEntity<>(null), String.class);

            if (re.getStatusCode().is2xxSuccessful()) {
                return Optional.ofNullable(re.getBody());
            } else {
                return Optional.empty();
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean verify(String safToken) {
        try {
            ResponseEntity<String> re = restTemplate.exchange(verifyUrl, HttpMethod.POST,
                new HttpEntity<>(null), String.class);

            return re.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized e) {
            return false;
        }
    }
}
