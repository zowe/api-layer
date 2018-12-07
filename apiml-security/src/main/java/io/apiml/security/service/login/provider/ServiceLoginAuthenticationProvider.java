/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.login.provider;

import io.apiml.security.service.authentication.ApimlAuthentication;
import io.apiml.security.service.authentication.ServiceLoginAuthentication;
import io.apiml.security.service.login.service.UserNotFoundException;
import io.apiml.security.service.login.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class ServiceLoginAuthenticationProvider implements AuthenticationProvider {
    private final UserService userService;

    public ServiceLoginAuthenticationProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ServiceLoginAuthentication loginAuthentication = (ServiceLoginAuthentication) authentication;
        try {
            String token = userService.login(loginAuthentication.getName(), loginAuthentication.getCredentials());
            ApimlAuthentication apimlAuthentication = new ApimlAuthentication(loginAuthentication.getName(), token);
            apimlAuthentication.setAuthenticated(true);
            return apimlAuthentication;
        } catch (UserNotFoundException e) {
            throw new BadCredentialsException("Username or password is incorrect");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ServiceLoginAuthentication.class.isAssignableFrom(authentication);
    }
}
