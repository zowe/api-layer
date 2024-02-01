/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.springframework.boot.actuate.trace.http;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public enum Include {
    REQUEST_HEADERS,
    RESPONSE_HEADERS,
    COOKIE_HEADERS,
    AUTHORIZATION_HEADER,
    PRINCIPAL,
    REMOTE_ADDRESS,
    SESSION_ID,
    TIME_TAKEN;

    private static final Set<Include> DEFAULT_INCLUDES;

    private Include() {
    }

    public static Set<Include> defaultIncludes() {
        return DEFAULT_INCLUDES;
    }

    static {
        Set<Include> defaultIncludes = new LinkedHashSet();
        defaultIncludes.add(REQUEST_HEADERS);
        defaultIncludes.add(RESPONSE_HEADERS);
        defaultIncludes.add(TIME_TAKEN);
        DEFAULT_INCLUDES = Collections.unmodifiableSet(defaultIncludes);
    }
}
