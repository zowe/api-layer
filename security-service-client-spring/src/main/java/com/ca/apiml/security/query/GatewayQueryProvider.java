/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.query;

import com.ca.apiml.security.service.GatewaySecurityService;
import com.ca.apiml.security.token.TokenAuthentication;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class GatewayQueryProvider implements AuthenticationProvider {

    private GatewaySecurityService gatewaySecurityService;

    public GatewayQueryProvider(GatewaySecurityService gatewaySecurityService) {
        this.gatewaySecurityService = gatewaySecurityService;
    }

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
