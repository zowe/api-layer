/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.query;

import io.apiml.security.service.authentication.ApimlAuthentication;
import io.apiml.security.service.authentication.TokenAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {
    private final GatewayQueryService gatewayQueryService;

    public TokenAuthenticationProvider(GatewayQueryService gatewayQueryService) {
        this.gatewayQueryService = gatewayQueryService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();
        ApimlAuthentication apimlAuthentication = gatewayQueryService.query(token);
        apimlAuthentication.setAuthenticated(true);
        return apimlAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}
