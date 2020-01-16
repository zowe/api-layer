/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.apiml.security.common.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public enum AuthenticationScheme {
    @JsonProperty("bypass")
    BYPASS("bypass"),

    @JsonProperty("zoweJwt")
    ZOWE_JWT("zoweJwt"),

    @JsonProperty("httpBasicPassTicket")
    HTTP_BASIC_PASSTICKET("httpBasicPassTicket"),

    @JsonProperty("zosmf")
    ZOSMF("zosmf");

    private final String scheme;

    private static Map<String, AuthenticationScheme> schemeToEnum;

    AuthenticationScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public String toString() {
        return scheme;
    }

    public static AuthenticationScheme fromScheme(String scheme) {
        if (schemeToEnum != null) return schemeToEnum.get(scheme);

        synchronized (AuthenticationScheme.class) {
            if (schemeToEnum != null) return schemeToEnum.get(scheme);

            final Map<String, AuthenticationScheme> map = new HashMap<>();
            for (final AuthenticationScheme as : values()) {
                map.put(as.scheme, as);
            }
            schemeToEnum = Collections.unmodifiableMap(map);

            return schemeToEnum.get(scheme);
        }
    }

}
