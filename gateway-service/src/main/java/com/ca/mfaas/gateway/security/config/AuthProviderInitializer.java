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
import com.ca.mfaas.gateway.security.query.TokenAuthenticationProvider;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.stereotype.Component;

/**
 * Initialize authentication and authorization provider set by apiml.security.auth.provider parameter
 */
@Component
public class AuthProviderInitializer {

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private final String authProvider;

    private final DummyAuthenticationProvider dummyAuthenticationProvider;
    private final ZosmfAuthenticationProvider zosmfAuthenticationProvider;

    private final TokenAuthenticationProvider tokenAuthenticationProvider;

    public AuthProviderInitializer(DummyAuthenticationProvider dummyAuthenticationProvider,
                                   ZosmfAuthenticationProvider zosmfAuthenticationProvider,
                                   TokenAuthenticationProvider tokenAuthenticationProvider,
                                   @Value("${apiml.security.auth.provider:zosmf}") String authProvider) {
        this.dummyAuthenticationProvider = dummyAuthenticationProvider;
        this.zosmfAuthenticationProvider = zosmfAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.authProvider = authProvider;
    }

    /**
     * Configure security providers:
     * 1. {@link ZosmfAuthenticationProvider} or {@link DummyAuthenticationProvider} for login credentials
     * 2. {@link TokenAuthenticationProvider} for query token
     *
     * @param auth authenticationManagerBuilder which is being configured
     */
    public void configure(AuthenticationManagerBuilder auth) {
        LoginProvider provider = getLoginProvider();
        if (provider.equals(LoginProvider.ZOSMF)) {
            auth.authenticationProvider(zosmfAuthenticationProvider);
        } else if (provider.equals(LoginProvider.DUMMY)) {
            apimlLog.log("apiml.security.loginEndpointInDummyMode");
            auth.authenticationProvider(dummyAuthenticationProvider);
        }
        auth.authenticationProvider(tokenAuthenticationProvider);
    }

    /**
     * Get login provider according apiml.security.auth.provider parameter
     *
     * @return login provider
     */
    private LoginProvider getLoginProvider() {
        LoginProvider provider = LoginProvider.ZOSMF;
        try {
            provider = LoginProvider.getLoginProvider(authProvider);
        } catch (IllegalArgumentException ex) {
            apimlLog.log("apiml.security.invalidAuthenticationProvider", authProvider);
        }
        return provider;
    }
}
