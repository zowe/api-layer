/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about expected authentication scheme and APPLID for PassTickets generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Authentication {

    private AuthenticationScheme scheme;
    private String applid;

    public boolean isEmpty() {
        return (scheme == null) && (applid == null);
    }
}
