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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdArraySerializers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * Authentication provider implementation for the SafIdt Tokens that gets and verifies the tokens across the Restfull
 * interface
 * <p>
 * To work properly the implementation requires two urls:
 * <p>
 * - apiml.security.saf.urls.authenticate - URL to generate token
 * - apiml.security.saf.urls.verify - URL to verify the validity of the token
 */
@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "apiml.security.saf.provider", havingValue = "rest")
public class SafRestAuthenticationService implements SafIdtProvider {

    private final PassTicketService passTicketService;
    private final RestTemplate restTemplate;

    static final HttpHeaders HEADER = new HttpHeaders();

    static {
        HEADER.setContentType(MediaType.APPLICATION_JSON);
        HEADER.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    @Value("${apiml.security.saf.urls.authenticate}")
    String authenticationUrl;
    @Value("${apiml.security.saf.urls.verify}")
    String verifyUrl;

    @Override
    public String generate(String username, String applId) {
        try {
            char[] passTicket = passTicketService.generate(username, applId).toCharArray();
            try {
                return generate(username, passTicket, applId);
            } finally {
                Arrays.fill(passTicket, (char) 0);
            }
        } catch (IRRPassTicketGenerationException e) {
            throw new PassTicketException(
                    String.format("Could not generate PassTicket for user ID '%s' and APPLID '%s'", username, applId), e
            );
        }
    }

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
                    new HttpEntity<>(authentication, HEADER),
                    Token.class);

            Token responseBody = response.getBody();
            if (responseBody == null) {
                throw new SafIdtException("ZSS authentication service has not returned the Identity token");
            }

            return responseBody.getJwt();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new SafIdtException("Unable to connect to ZSS authentication service", e);
        }
    }

    @Override
    public boolean verify(String safToken, String applid) {
        if (isEmpty(safToken)) {
            return false;
        }

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    URI.create(verifyUrl),
                    HttpMethod.POST,
                    new HttpEntity<>(new Token(safToken, applid), HEADER),
                    Void.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class Token {
        String jwt;
        String applid;
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
