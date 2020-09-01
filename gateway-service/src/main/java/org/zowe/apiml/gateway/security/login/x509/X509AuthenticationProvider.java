/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.ZosmfService;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfServiceFacade;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;

import static org.zowe.apiml.gateway.security.service.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.gateway.security.service.ZosmfService.TokenType.LTPA;

@Component
@RequiredArgsConstructor
public class X509AuthenticationProvider implements AuthenticationProvider {

    @Value("${apiml.security.zosmf.useJwtToken:true}")
    private boolean useJwtToken;

    private final X509Authentication x509Authentication;
    private final AuthenticationService authenticationService;
    private final PassTicketService passTicketService;
    private final ZosmfServiceFacade zosmfServiceFacade;
    protected final AuthConfigurationProperties authConfigurationProperties;

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (authentication instanceof X509AuthenticationToken) {
            X509Certificate[] certs = (X509Certificate[]) authentication.getCredentials();
            String username = x509Authentication.mapUserToCertificate(certs[0]);
            if (username == null) {
                return null;
            }
            // TODO: How do we verify presence of zOSMF? I don't think we have this implemented so far.
            boolean isZosmfPresent = authConfigurationProperties.getProvider().equals("zosmf");

            if (isZosmfPresent) {
                try {
                    String passTicket = passTicketService.generate(username, "IZUDFLT");
                    final ZosmfService.AuthenticationResponse ar =
                        zosmfServiceFacade.authenticate(new UsernamePasswordAuthenticationToken(username, passTicket));

                    // if z/OSMF support JWT, use it as Zowe JWT token
                    if (ar.getTokens().containsKey(JWT) && useJwtToken) {
                        return authenticationService.createTokenAuthentication(username, ar.getTokens().get(JWT));
                    }

                    if (ar.getTokens().containsKey(LTPA)) {
                        // construct own JWT token, including LTPA from z/OSMF
                        final String domain = ar.getDomain();
                        final String jwtToken = authenticationService.createJwtToken(username, domain, ar.getTokens().get(LTPA));

                        return authenticationService.createTokenAuthentication(username, jwtToken);
                    }

                    throw new BadCredentialsException("Username or password are invalid.");
                } catch (IRRPassTicketGenerationException e) {
                    e.printStackTrace();
                    throw new AuthenticationTokenException("Wrong authentication token. " + authentication.getClass());
                }
            } else {
                // If only SAF is present this is totally valid
                final String domain = "security-domain";
                final String jwtToken = authenticationService.createJwtToken(username, domain, null);
                return authenticationService.createTokenAuthentication(username, jwtToken);
            }
        } else {
            throw new AuthenticationTokenException("Wrong authentication token. " + authentication.getClass());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return X509AuthenticationToken.class.isAssignableFrom(authentication);
    }
}

