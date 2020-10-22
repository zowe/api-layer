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
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;

@RequiredArgsConstructor
@Slf4j
@Service
public class TokenCreationService {
    private final Providers providers;
    private final ZosmfAuthenticationProvider zosmfAuthenticationProvider;
    private final PassTicketService passTicketService;
    private final AuthenticationService authenticationService;

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
        boolean isZosmfUsedAndAvailable = false;
        log.error("Creating JWT");
        try {
            isZosmfUsedAndAvailable = providers.isZosfmUsed() && providers.isZosmfAvailable();
            log.error("Is zosmf available: " + isZosmfUsedAndAvailable);
        } catch (AuthenticationServiceException ex) {
            // Intentionally do nothing. The issue is logged deeper.
        }

        if (isZosmfUsedAndAvailable) {
            try {
                String passTicket = passTicketService.generate(user, zosmfApplId);
                log.error("Pass ticket for zosmf: {}", passTicket);
                return ((TokenAuthentication) zosmfAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(user, passTicket)))
                    .getCredentials();
            } catch (IRRPassTicketGenerationException e) {
                log.error("error generating zosmf", e);
                throw new AuthenticationTokenException("Problem with generating PassTicket");
            }
        } else {
            final String domain = "security-domain";
            final String jwtTokenString = authenticationService.createJwtToken(user, domain, null);
            return authenticationService.createTokenAuthentication(user, jwtTokenString).getCredentials();
        }
    }
}
