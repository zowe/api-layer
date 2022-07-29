/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationSchemes {
    private final Map<String, AuthenticationScheme> schemeToEnum;

    public AuthenticationSchemes() {
        final Map<String, AuthenticationScheme> map = new HashMap<>();
        for (final AuthenticationScheme as : AuthenticationScheme.values()) {
            map.put(as.scheme, as);
        }
        schemeToEnum = Collections.unmodifiableMap(map);
    }

    public AuthenticationScheme map(String schemeName) {
        return schemeToEnum.get(schemeName);
    }
}
