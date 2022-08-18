/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
public class AccessTokenContainer {

    public AccessTokenContainer() {
        // no args constructor
    }

    private String userId;
    private String tokenValue;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private Set<String> scopes;
    private String tokenProvider;

}
