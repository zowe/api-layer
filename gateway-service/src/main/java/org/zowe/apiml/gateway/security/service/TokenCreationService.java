/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class TokenCreationService {
    private final Providers providers;
    private final Optional<ZosmfAuthenticationProvider> zosmfAuthenticationProvider;
    private final ZosmfService zosmfService;
    private final PassTicketService passTicketService;
    private final AuthenticationService authenticationService;
    private final SafIdtProvider safIdtProvider;

    @Value("${apiml.security.zosmf.applid:IZUDFLT}")
    protected String zosmfApplId;

    /**
     * Creates valid JWT token without using any credentials. The token will be valid for zOSMF as well as for southbound
     * services
     *
     * @param user Username to create the JWT token for.
     * @return Valid JWT token or null
     */
    public String createJwtTokenWithoutCredentials(String user) {
        if (isZosmfAvailable()) {
            log.debug("ZOSMF is available and used. Attempt to authenticate with PassTicket");
            final String passTicket = generatePassTicket(user);
            return ((TokenAuthentication) zosmfAuthenticationProvider
                .orElseThrow(() -> new IllegalStateException("The z/OSMF is not configured. The config value `apiml.security.auth.provider` should be set to `zosmf`."))
                .authenticate(new UsernamePasswordAuthenticationToken(user, passTicket)))
                .getCredentials();
        } else {
            final String domain = "security-domain";
            log.debug("ZOSMF is not available or used. Generating APIML's JWT Token.");
            final String jwtTokenString = authenticationService.createJwtToken(user, domain, null);
            log.debug("Generated JWT Token: {}", jwtTokenString);
            return authenticationService.createTokenAuthentication(user, jwtTokenString).getCredentials();
        }
    }

    public Map<ZosmfService.TokenType, String> createZosmfTokensWithoutCredentials(String user) {
        if (!isZosmfAvailable()) return Collections.emptyMap();

        log.debug("ZOSMF is available and used. Attempt to authenticate with PassTicket");
        final String passTicket = generatePassTicket(user);

        return zosmfService.authenticate(new UsernamePasswordAuthenticationToken(user, passTicket)).getTokens();
    }

    public String createSafIdTokenWithoutCredentials(String user, String applId) throws IRRPassTicketGenerationException {

        char[] passTicket = "".toCharArray();
        try {
            passTicket = passTicketService.generate(user, applId).toCharArray();
            return safIdtProvider.generate(user, passTicket, applId);
        } finally {
            Arrays.fill(passTicket, (char) 0);
        }
    }

    private boolean isZosmfAvailable() {
        try {
            return providers.isZosfmUsed() && providers.isZosmfAvailable();
        } catch (AuthenticationServiceException ex) {
            // Intentionally do nothing. The issue is logged deeper.
        }

        return false;
    }

    private String generatePassTicket(String user) {
        try {
            log.debug("Generating PassTicket for user: {} and ZOSMF applid: {}", user, zosmfApplId);
            String passTicket = passTicketService.generate(user, zosmfApplId);
            log.debug("Generated PassTicket: {}", passTicket);

            return passTicket;
        } catch (IRRPassTicketGenerationException e) {
            throw new AuthenticationTokenException("Generation of PassTicket failed", e);
        }
    }

}
