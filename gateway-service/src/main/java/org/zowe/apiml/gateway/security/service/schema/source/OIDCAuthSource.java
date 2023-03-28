/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema.source;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * Implementation of OIDC token source of authentication.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OIDCAuthSource implements AuthSource {
    public static final AuthSource.AuthSourceType type = AuthSource.AuthSourceType.OIDC;

    @EqualsAndHashCode.Include
    private final String source;

    @Override
    public String getRawSource() {
        return source;
    }

    @Override
    public AuthSource.AuthSourceType getType() {
        return type;
    }

    @Setter
    private String distributedId;

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class Parsed implements AuthSource.Parsed {
        private final String userId;
        private final Date creation;
        private final Date expiration;
        private final AuthSource.Origin origin;
    }
}
