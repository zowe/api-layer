/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.config;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.login.dummy.DummyAuthenticationProvider;
import org.zowe.apiml.gateway.security.login.zosmf.ZosmfAuthenticationProvider;
import org.zowe.apiml.gateway.security.query.TokenAuthenticationProvider;

/**
 * Initialize authentication and authorization provider set by apiml.security.auth.provider parameter
 */
@Component
public class AuthProviderInitializer {

    private final CompoundAuthProvider loginAuthProvider;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;

    public AuthProviderInitializer(CompoundAuthProvider loginAuthProvider,
                                   TokenAuthenticationProvider tokenAuthenticationProvider) {
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.loginAuthProvider = loginAuthProvider;
    }

    /**
     * Configure security providers:
     * 1. {@link ZosmfAuthenticationProvider} or {@link DummyAuthenticationProvider} or {@link org.zowe.apiml.gateway.security.login.saf.ZosAuthenticationProvider} for login credentials
     * 2. {@link TokenAuthenticationProvider} for query token
     * 3. {@link PreAuthenticatedAuthenticationProvider} for cert login
     *
     * @param auth authenticationManagerBuilder which is being configured
     */
    public void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(loginAuthProvider);
        auth.authenticationProvider(tokenAuthenticationProvider);

        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(new SimpleUserDetailService());

        auth.authenticationProvider(provider);
    }
}
