/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.x509;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.security.common.error.AuthenticationTokenException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import java.security.cert.X509Certificate;

@Component
@Slf4j
@RequiredArgsConstructor
public class X509AuthenticationProvider implements AuthenticationProvider {

    @Value("${apiml.security.x509.enabled:false}")
    boolean isClientCertEnabled;

    @Qualifier("x509Mapper")
    private final AuthenticationMapper x509AuthenticationMapper;
    private final TokenCreationService tokenCreationService;

    /**
     * Performs Authentication of Client certificate
     * <p>
     * Maps certificate to mainframe UserId
     * <p>
     * If z/OSMF is active, authenticate against it with passticket.
     * Otherwise, the fact that mapping happened is proof of authentication
     * If SAF or DUMMY auth providers are selected, they defer the decision to the mapping component.
     * For list of mapping components, see implementations of {@link AuthenticationMapper}
     *
     * @param authentication {@link Authentication}
     * @return {@link Authentication}
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        log.debug("Authenticating X509 Certificate");
        if (authentication instanceof X509AuthenticationToken) {
            if (!isClientCertEnabled) {
                log.debug("X509 authentication is not enabled. Certificate will not be authenticated.");
                return null;
            }
            String username = getUserid(authentication);
            if (username == null) {
                log.debug("Mapping user to certificate was not successful.");
                return null;
            }
            log.debug("Successfully mapped user to certificate: {}", username);
            String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(username);
            Authentication tokenAuthentication = new TokenAuthentication(username, jwtToken);
            tokenAuthentication.setAuthenticated(true);
            log.debug("Successfully authenticated user {} by X509 certificate.", username);
            return tokenAuthentication;
        } else {
            // This should never happen because of supports method which should fail.
            throw new AuthenticationTokenException("Wrong authentication token. " + authentication.getClass());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return X509AuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String getUserid(Authentication authentication) {
        X509Certificate[] certs = (X509Certificate[]) authentication.getCredentials();
        log.debug("Getting user id for certificate: {}", certs[0]);
        return x509AuthenticationMapper.mapToMainframeUserId(new X509AuthSource(certs[0]));
    }
}

