/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login;

/**
 * Represents supported login providers
 */
public enum LoginProvider {
    ZOSMF("zosmf","zosmfAuthenticationProvider"),
    SAF("saf","zosAuthenticationProvider"),
    DUMMY("dummy", "dummyAuthenticationProvider");

    private final String value;
    private final String authProviderBeanName;

    LoginProvider(String value, String authProviderBeanName) {
        this.value = value;
        this.authProviderBeanName = authProviderBeanName;
    }

    public String getValue() {
        return value;
    }

    public String getAuthProviderBeanName() {
        return authProviderBeanName;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    public static LoginProvider getLoginProvider(String value) {
        for (LoginProvider provider : values()) {
            if (provider.getValue().equalsIgnoreCase(value)) {
                return provider;
            }
        }

        return null;
    }
}
