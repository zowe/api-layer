/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Implementation of JWT token source of authentication.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JwtAuthSource implements AuthSource {
    public static final AuthSourceType type = AuthSourceType.JWT;

    /**
     * JWT token
     */
    @EqualsAndHashCode.Include
    private final String source;

    @Override
    public String getRawSource() {
        return source;
    }

    @Override
    public AuthSourceType getType() {
        return type;
    }

}
