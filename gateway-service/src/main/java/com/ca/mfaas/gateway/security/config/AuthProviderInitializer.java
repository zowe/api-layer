/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.config;

import com.ca.mfaas.gateway.security.login.LoginProvider;
import com.ca.mfaas.gateway.security.login.dummy.DummyAuthenticationProvider;
import com.ca.mfaas.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import com.ca.mfaas.gateway.security.token.TokenAuthenticationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthProviderInitializer {

    private final String authProvider;

    private final DummyAuthenticationProvider dummyAuthenticationProvider;
    private final ZosmfAuthenticationProvider zosmfAuthenticationProvider;

    private final TokenAuthenticationProvider tokenAuthenticationProvider;

    public AuthProviderInitializer(DummyAuthenticationProvider dummyAuthenticationProvider,
                                   ZosmfAuthenticationProvider zosmfAuthenticationProvider,
                                   TokenAuthenticationProvider tokenAuthenticationProvider,
                                   @Value("${apiml.security.auth.provider}") String authProvider) {
        this.dummyAuthenticationProvider = dummyAuthenticationProvider;
        this.zosmfAuthenticationProvider = zosmfAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.authProvider = authProvider;
    }

    public void configure(AuthenticationManagerBuilder auth) {
        LoginProvider provider = getLoginProvider();
        switch (provider) {
            case ZOSMF:
                auth.authenticationProvider(zosmfAuthenticationProvider);
                break;
            case DUMMY:
                log.warn("Login endpoint is running in the dummy mode. Use credentials user/user to login.");
                log.warn("Do not use this option in the production environment.");
                auth.authenticationProvider(dummyAuthenticationProvider);
                break;
            default:
                log.warn("Unsupported value of authentication provider indicated in apiml.security.auth.provider = {}.", provider.getValue());
                log.warn("Default 'zosmf' authentication provider is used.");
                auth.authenticationProvider(zosmfAuthenticationProvider);
        }
        auth.authenticationProvider(tokenAuthenticationProvider);
    }

    private LoginProvider getLoginProvider() {
        LoginProvider provider = LoginProvider.ZOSMF;
        try {
            provider = LoginProvider.getLoginProvider(authProvider);
        } catch (IllegalArgumentException ex) {
            log.warn("Authentication provider is not set correctly. Default 'zosmf' authentication provider is used.");
            log.warn("Incorrect value: apiml.security.auth.provider = {}", authProvider);
        }
        return provider;
    }
}
