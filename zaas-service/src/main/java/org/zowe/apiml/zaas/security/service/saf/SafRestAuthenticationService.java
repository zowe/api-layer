/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.saf;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdArraySerializers;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;

import static org.springframework.util.StringUtils.hasLength;

/**
 * Authentication provider implementation for the SafIdt Tokens that gets and verifies the tokens across the Restfull
 * interface
 * <p>
 * To work properly the implementation requires two urls:
 * <p>
 * - apiml.security.saf.urls.authenticate - URL to generate token
 * - apiml.security.saf.urls.verify - URL to verify the validity of the token
 */
@RequiredArgsConstructor
@Slf4j
public class SafRestAuthenticationService implements SafIdtProvider {

    private final RestTemplate restTemplate;

    static final HttpHeaders HEADERS = new HttpHeaders();

    static {
        HEADERS.setContentType(MediaType.APPLICATION_JSON);
        HEADERS.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    @Value("${apiml.security.saf.urls.authenticate}")
    String authenticationUrl;
    @Value("${apiml.security.saf.urls.verify}")
    String verifyUrl;

    @Override
    public String generate(String username, char[] password, String applId) {
        Authentication authentication = Authentication.builder()
            .username(username)
            .pass(password)
            .appl(applId)
            .build();

        try {
            ResponseEntity<Token> response = restTemplate.exchange(
                URI.create(authenticationUrl),
                HttpMethod.POST,
                new HttpEntity<>(authentication, HEADERS),
                Token.class);
            if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getStatusCode())) {
                log.debug("The request with URL {} used to generate the SAF IDT token failed with response code {}.", authenticationUrl, response.getStatusCode());
                if (response.getBody() != null) {    //NOSONAR tests return null
                    throw new SafIdtException(response.getBody().toString());  //NOSONAR tests return null
                }
                throw new SafIdtException("Cannot connect to ZSS authentication service and generate the SAF IDT token. Please, verify your configuration.");
            }
            Token responseBody = response.getBody();
            if (responseBody == null || StringUtils.isEmpty(responseBody.getJwt())) {
                throw new SafIdtException("ZSS authentication service has not returned the Identity token");
            }

            return responseBody.getJwt();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new SafIdtAuthException("Authentication to ZSS failed", e);
        }
    }

    @Override
    public boolean verify(String safToken, String applid) {
        if (!hasLength(safToken)) {
            return false;
        }

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                URI.create(verifyUrl),
                HttpMethod.POST,
                new HttpEntity<>(new Token(safToken, applid), HEADERS),
                Void.class);

            if (HttpStatus.INTERNAL_SERVER_ERROR.equals(response.getStatusCode())) {
                log.debug("The request with URL {} used to validate the SAF IDT token failed with response code {}.", verifyUrl, response.getStatusCode());
                throw new SafIdtException("Cannot connect to ZSS authentication service and validate the SAF IDT token. Please, verify your configuration.");
            }
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Token {
        String jwt;
        String appl;
    }

    @lombok.Value
    @Builder
    public static class Authentication {
        String username;
        @JsonSerialize(using = StdArraySerializers.CharArraySerializer.class)
        char[] pass;
        String appl;
    }

}
