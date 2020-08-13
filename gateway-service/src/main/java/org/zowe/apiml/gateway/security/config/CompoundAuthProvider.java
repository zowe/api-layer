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

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.util.Arrays;
import java.util.Map;

@Component
@Slf4j
public class CompoundAuthProvider implements AuthenticationProvider {

    public static final String ORG_ZOWE_APIML_SECURITY_INVALID_AUTHENTICATION_PROVIDER = "org.zowe.apiml.security.invalidAuthenticationProvider";
    public static final String APIML_SECURITY_AUTH_PROVIDER = "apiml.security.auth.provider";
    public static final String DEFAULT_AUTH_PROVIDER = "zosmf";
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();
    private final Map<String, AuthenticationProvider> authProvidersMap;
    private final Environment environment;
    private LoginProvider loginProvider;

    public CompoundAuthProvider(Map<String, AuthenticationProvider> authProvidersMap, Environment environment) {
        this.authProvidersMap = authProvidersMap;
        this.environment = environment;
        final String authProvider = environment.getProperty(APIML_SECURITY_AUTH_PROVIDER, DEFAULT_AUTH_PROVIDER);
        try {
            loginProvider = LoginProvider.getLoginProvider(authProvider);
        } catch (IllegalArgumentException ex) {
            apimlLog.log(ORG_ZOWE_APIML_SECURITY_INVALID_AUTHENTICATION_PROVIDER, authProvider);
        }
    }

    public AuthenticationProvider getConfiguredLoginAuthProvider() {
        final AuthenticationProvider currentAuthProvider = authProvidersMap.get(loginProvider.getAuthProviderBeanName());
        return currentAuthProvider;
    }

    public String getLoginAuthProviderName() {
        return loginProvider.getValue();
    }

    public void setLoginAuthProvider(LoginProvider loginAuthProvider) {
        if ((environment != null) && Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            this.loginProvider = loginAuthProvider;
        } else {
            log.warn("Login Authentication provider can't be changed at runtime in the current profile.");
        }
    }


    /**
     * Performs authentication with the same contract as
     * {@link AuthenticationManager#authenticate(Authentication)}
     * .
     *
     * @param authentication the authentication request object.
     * @return a fully authenticated object including credentials. May return
     * <code>null</code> if the <code>AuthenticationProvider</code> is unable to support
     * authentication of the passed <code>Authentication</code> object. In such a case,
     * the next <code>AuthenticationProvider</code> that supports the presented
     * <code>Authentication</code> class will be tried.
     * @throws AuthenticationException if authentication fails.
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        AuthenticationProvider configuredLoginAuthProvider = getConfiguredLoginAuthProvider();
        return configuredLoginAuthProvider.authenticate(authentication);
    }

    /**
     * Returns <code>true</code> if this <Code>AuthenticationProvider</code> supports the
     * indicated <Code>Authentication</code> object.
     * <p>
     * Returning <code>true</code> does not guarantee an
     * <code>AuthenticationProvider</code> will be able to authenticate the presented
     * instance of the <code>Authentication</code> class. It simply indicates it can
     * support closer evaluation of it. An <code>AuthenticationProvider</code> can still
     * return <code>null</code> from the {@link #authenticate(Authentication)} method to
     * indicate another <code>AuthenticationProvider</code> should be tried.
     * </p>
     * <p>
     * Selection of an <code>AuthenticationProvider</code> capable of performing
     * authentication is conducted at runtime the <code>ProviderManager</code>.
     * </p>
     *
     * @param authentication that was presented to the provider for validation
     * @return <code>true</code> if the implementation can more closely evaluate the
     * <code>Authentication</code> class presented
     */
    @Override
    public boolean supports(Class<?> authentication) {
        AuthenticationProvider configuredLoginAuthProvider = getConfiguredLoginAuthProvider();
        return configuredLoginAuthProvider.supports(authentication);
    }
}
