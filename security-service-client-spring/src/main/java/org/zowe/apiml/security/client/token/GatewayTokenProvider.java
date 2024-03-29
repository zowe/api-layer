/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.client.token;

import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authentication provider that authenticates TokenAuthentication against Gateway
 */
@Component
@RequiredArgsConstructor
public class GatewayTokenProvider implements AuthenticationProvider {
    private final GatewaySecurityService gatewaySecurityService;

    /**
     * Authenticate the token
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        QueryResponse queryResponse = gatewaySecurityService.query(tokenAuthentication.getCredentials());

        TokenAuthentication validTokenAuthentication = new TokenAuthentication(queryResponse.getUserId(), tokenAuthentication.getCredentials());
        validTokenAuthentication.setAuthenticated(true);

        return validTokenAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}
