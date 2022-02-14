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

import java.util.Date;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.zowe.apiml.security.common.token.QueryResponse.Source;

/**
 * Implementation of JWT token source of authentication.
 */
@RequiredArgsConstructor
@Getter
public class JwtAuthSource implements AuthSource {
    private final String source;

    @Override
    public String getRawSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JwtAuthSource that = (JwtAuthSource) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class Parsed implements AuthSource.Parsed {
        private final String userId;
        private final Date creation;
        private final Date expiration;
        private final Source source;
    }
}
