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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class X509AuthenticationProvider implements AuthenticationProvider {

    private final X509Authentication x509Authentication;
    private final AuthenticationService authenticationService;

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (authentication instanceof X509AuthenticationToken) {
            X509Certificate[] certs = (X509Certificate[]) authentication.getCredentials();
            String username = x509Authentication.verifyCertificate(certs[0]);
            if (!"user".equals(username)) {
                return null;
            }
            final String domain = "security-domain";
            final String jwtToken = authenticationService.createJwtToken(username, domain, null);
            return authenticationService.createTokenAuthentication(username, jwtToken);
        } else {
            throw new AuthenticationTokenException("Wrong authentication token. " + authentication.getClass());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return X509AuthenticationToken.class.isAssignableFrom(authentication);
    }
}

