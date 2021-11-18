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

import com.netflix.zuul.context.RequestContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.net.URI;
import java.util.Optional;

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
@RequiredArgsConstructor
@Slf4j
public class SafRestAuthenticationService implements SafIdtProvider {
    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;
    private final PassTicketService passTicketService;

    @Value("${apiml.security.saf.urls.authenticate}")
    String authenticationUrl;
    @Value("${apiml.security.saf.urls.verify}")
    String verifyUrl;
    @Value("${apiml.security.zosmf.applid:IZUDFLT}")
    protected String zosmfApplId;

    @Override
    public Optional<String> generate(String username) {
        final RequestContext context = RequestContext.getCurrentContext();
        Optional<String> jwtToken = authenticationService.getJwtTokenFromRequest(context.getRequest());
        if (!jwtToken.isPresent()) {
            return Optional.empty();
        }

        TokenAuthentication tokenAuthentication = authenticationService.validateJwtToken(jwtToken.get());
        if (!tokenAuthentication.isAuthenticated()) {
            return Optional.empty();
        }

        try {
            Authentication authentication = new Authentication();
            authentication.setJwt(jwtToken.get());
            authentication.setUsername(username);
            String passTicket = passTicketService.generate(username, zosmfApplId);
            log.debug("Generated passticket: {}", passTicket);
            authentication.setPass(passTicket);

            ResponseEntity<Token> re = restTemplate.postForEntity(URI.create(authenticationUrl), authentication, Token.class);

            if (!re.getStatusCode().is2xxSuccessful()) {
                return Optional.empty();
            }

            Token responseBody = re.getBody();
            if (responseBody == null) {
                return Optional.empty();
            }

            return Optional.of(responseBody.getJwt());
        } catch (HttpClientErrorException.Unauthorized e) {
            return Optional.empty();
        }
        catch (IRRPassTicketGenerationException e) {
            throw new AuthenticationTokenException("Problem with generating PassTicket");
        }
    }

    @Override
    public boolean verify(String safToken) {
        if (isEmpty(safToken)) {
            return false;
        }

        try {
            Token token = new Token();
            token.setJwt(safToken);

            ResponseEntity<String> re = restTemplate.postForEntity(URI.create(verifyUrl), token, String.class);

            return re.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.Unauthorized e) {
            return false;
        }
    }

    @Data
    public static class Token {
        String jwt;
    }

    @Data
    public static class Authentication {
        String jwt;
        String username;
        String pass;
    }
}
