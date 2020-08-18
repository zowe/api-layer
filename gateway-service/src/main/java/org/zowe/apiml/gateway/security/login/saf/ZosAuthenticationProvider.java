
/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.saf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.service.AuthenticationService;

@Component
@Slf4j
@RequiredArgsConstructor
public class ZosAuthenticationProvider implements AuthenticationProvider, InitializingBean {
    private PlatformUser platformUser = null;

    private final AuthenticationService authenticationService;

    @Override
    public Authentication authenticate(Authentication authentication) {
        String userid = authentication.getName();
        String password = authentication.getCredentials().toString();
        PlatformReturned returned = (PlatformReturned) getPlatformUser().authenticate(userid, password);

        if ((returned == null) || (returned.isSuccess())) {
            final String domain = "security-domain";
            final String jwtToken = authenticationService.createJwtToken(userid, domain, "ltpa");
            return authenticationService.createTokenAuthentication(userid, jwtToken);
        } else {
            throw new BadCredentialsException("Username or password are invalid.");
        }
    }

    private PlatformUser getPlatformUser() {
        return platformUser;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    @Override
    public void afterPropertiesSet() {
        if (platformUser == null) {
                platformUser = new SafPlatformUser(new SafPlatformClassFactory());
        }
    }
}
