/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.login;

public enum LoginProvider {
    ZOSMF("zosmf"),
    DUMMY("dummy");

    private final String value;

    LoginProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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
        throw new IllegalArgumentException();
    }
}
