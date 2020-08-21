/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.client.login;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.client.service.GatewaySecurityService;

@Component
@RequiredArgsConstructor
public class GatewayX509AuthProvider implements AuthenticationProvider {

    private final GatewaySecurityService gatewaySecurityService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
       return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return false;
    }
}
