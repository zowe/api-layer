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
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class X509AuthenticationProvider implements AuthenticationProvider {

    @Value("${apiml.security.zosmf.applid:IZUDFLT}")
    private String zosmfApplId;
    @Value("${apiml.security.x509.enabled:false}")
    boolean isClientCertEnabled;

    private final Map<String, X509AuthenticationMapper> x509AuthenticationMapper;
    private final AuthenticationService authenticationService;

    private final PassTicketService passTicketService;
    private final ZosmfAuthenticationProvider zosmfAuthenticationProvider;
    private final Providers providers;

    /**
     * Performs Authentication of Client certificate
     * <p>
     * Maps certificate to mainframe UserId
     * <p>
     * If z/OSMF is active, authenticate against it with passticket.
     * Otherwise, the fact that mapping happened is proof of authentication
     * If SAF or DUMMY auth providers are selected, they defer the decision to the mapping component.
     * For list of mapping components, see implementations of {@link X509AuthenticationMapper}
     *
     * @param {@link Authentication}
     * @return {@link Authentication}
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        if (authentication instanceof X509AuthenticationToken) {
            if (!isClientCertEnabled) {
                return null;
            }
            boolean isZosmfUsedAndAvailable = false;
            try {
                isZosmfUsedAndAvailable = providers.isZosfmUsed() && providers.isZosmfAvailable();
            } catch (AuthenticationServiceException ex) {
                // Intentionally do nothing. The issue is logged deeper.
            }
            X509Certificate[] certs = (X509Certificate[]) authentication.getCredentials();
            String providerName = isZosmfUsedAndAvailable ? "externalMapper" : "commonNameMapper";
            String username = x509AuthenticationMapper.get(providerName).mapCertificateToMainframeUserId(certs[0]);
            if (username == null) {
                return null;
            }
            if (isZosmfUsedAndAvailable) {
                try {
                    String passTicket = passTicketService.generate(username, zosmfApplId);
                    return zosmfAuthenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(username, passTicket));
                } catch (IRRPassTicketGenerationException e) {
                    throw new AuthenticationTokenException("Problem with generating PassTicket");
                }
            } else {
                final String domain = "security-domain";
                final String jwtToken = authenticationService.createJwtToken(username, domain, null);
                return authenticationService.createTokenAuthentication(username, jwtToken);
            }
        } else {
            // This should never happen because of supports method which should fail.
            throw new AuthenticationTokenException("Wrong authentication token. " + authentication.getClass());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return X509AuthenticationToken.class.isAssignableFrom(authentication);
    }
}

