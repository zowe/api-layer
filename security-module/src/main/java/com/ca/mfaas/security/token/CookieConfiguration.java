/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.token;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class CookieConfiguration {
    private final String name;
    private final boolean secure;
    private final String path;
    private final String comment;
    private final Integer maxAge;
}
