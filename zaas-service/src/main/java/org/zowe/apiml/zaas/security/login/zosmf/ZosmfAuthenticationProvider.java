/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.zosmf;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.InvalidTokenTypeException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

import static org.zowe.apiml.zaas.security.service.zosmf.ZosmfService.TokenType.JWT;
import static org.zowe.apiml.zaas.security.service.zosmf.ZosmfService.TokenType.LTPA;

/**
 * Authentication provider that verifies credentials against z/OSMF service
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "apiml.security.auth.provider", havingValue = "zosmf", matchIfMissing = true)
public class ZosmfAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationService authenticationService;
    private final ZosmfService zosmfService;
    private final AuthConfigurationProperties authConfigurationProperties;

    /**
     * Authenticate the credentials with the z/OSMF service
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        final String user = authentication.getPrincipal().toString();
        final char[] newPassword = LoginRequest.getNewPassword(authentication);
        if (ArrayUtils.isNotEmpty(newPassword)) {
            zosmfService.changePassword(authentication);
            authentication = new UsernamePasswordAuthenticationToken(user, newPassword);
        }
        final ZosmfService.AuthenticationResponse ar;
        try {
            ar = zosmfService.authenticate(authentication);
        } catch (TokenNotValidException e) {
            throw new BadCredentialsException("Invalid Credentials");
        }
        switch (authConfigurationProperties.getZosmf().getJwtAutoconfiguration()) {
            case LTPA:
                if (ar.getTokens().containsKey(LTPA)) {
                    return getApimlJwtToken(user, ar);
                } else if (ar.getTokens().containsKey(JWT)) {
                    throw new InvalidTokenTypeException("JWT token in z/OSMF response but configured to expect LTPA");
                }
                break;
            default:
                if (ar.getTokens().containsKey(JWT)) {
                    return getZosmfJwtToken(user, ar);
                } else if (ar.getTokens().containsKey(LTPA)) {
                    throw new InvalidTokenTypeException("LTPA token in z/OSMF response but configured to expect JWT");
                }
                break;
        }
      //   JWT and LTPA tokens are missing, authentication was wrong
        throw new BadCredentialsException("Invalid Credentials");
    }

    public TokenAuthentication getZosmfJwtToken(String user, ZosmfService.AuthenticationResponse ar) {
        return authenticationService.createTokenAuthentication(user, ar.getTokens().get(JWT));
    }

    private TokenAuthentication getApimlJwtToken(String user, ZosmfService.AuthenticationResponse ar) {
        final String domain = ar.getDomain();
        final String jwtToken = authenticationService.createJwtToken(user, domain, ar.getTokens().get(LTPA));

        return authenticationService.createTokenAuthentication(user, jwtToken);
    }

    @Override
    public boolean supports(Class<?> auth) {
        return auth == UsernamePasswordAuthenticationToken.class;
    }

}
