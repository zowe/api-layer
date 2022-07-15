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

import java.util.Date;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PATAuthSource implements AuthSource {
    public static AuthSource.AuthSourceType type = AuthSource.AuthSourceType.PAT;

    @EqualsAndHashCode.Include
    private final String source;

    @Override
    public Object getRawSource() {
        return source;
    }

    @Override
    public AuthSourceType getType() {
        return type;
    }

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class Parsed implements AuthSource.Parsed {
        private final String userId;
        private final Date creation;
        private final Date expiration;
        private final Origin origin;
    }
}
