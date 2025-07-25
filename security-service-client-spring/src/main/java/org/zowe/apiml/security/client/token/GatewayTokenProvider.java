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

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.client.service.GatewaySecurity;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;

/**
 * Authentication provider that authenticates TokenAuthentication against Gateway
 */
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "modulithConfig")
public class GatewayTokenProvider implements AuthenticationProvider {

    private final GatewaySecurity gatewaySecurity;

    /**
     * Authenticate the token
     *
     * @param authentication that was presented to the provider for validation
     * @return the authenticated token
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        QueryResponse queryResponse;
        if (tokenAuthentication.getType() == TokenAuthentication.Type.OIDC) {
            queryResponse = gatewaySecurity.verifyOidc(tokenAuthentication.getCredentials());
        } else {
            queryResponse = gatewaySecurity.query(tokenAuthentication.getCredentials());
        }

        TokenAuthentication validTokenAuthentication = new TokenAuthentication(queryResponse.getUserId(), tokenAuthentication.getCredentials(), tokenAuthentication.getType());
        validTokenAuthentication.setAuthenticated(true);

        return validTokenAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}
