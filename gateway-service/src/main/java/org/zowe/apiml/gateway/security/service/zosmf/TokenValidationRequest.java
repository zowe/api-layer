/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.zosmf;

import lombok.*;

import java.util.Map;

@Data
@RequiredArgsConstructor
public class TokenValidationRequest {
    private final ZosmfService.TokenType tokenType;
    private final String token;
    private final String zosmfBaseUrl;
    private final Map<String, Boolean> endpointExistenceMap;

    @Setter
    private STATUS authenticated = STATUS.UNKNOWN;

    public enum STATUS {
        AUTHENTICATED,
        INVALID,
        UNKNOWN
    }
}
