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

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Implementation of source of authentication based on client certificate.
 */
@RequiredArgsConstructor
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class X509AuthSource implements AuthSource {
    public static final AuthSourceType type = AuthSourceType.CLIENT_CERT;
    /**
     * X509 client certificate
     */
    @EqualsAndHashCode.Include
    private final X509Certificate source;

    @Override
    public X509Certificate getRawSource() {
        return source;
    }

    @Override
    public AuthSourceType getType() {
        return AuthSourceType.CLIENT_CERT;
    }

    @RequiredArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class Parsed implements AuthSource.Parsed, X509Parsed, Serializable {
        private static final long serialVersionUID = 8152448925361577715L;

        private final String userId;
        private final Date creation;
        private final Date expiration;
        private final Origin origin;
        private final String publicKey;
        private final String distinguishedName;

        public String getCommonName() {
            return userId;
        }
    }

    public interface X509Parsed {
        String getCommonName();
        String getPublicKey();
        String getDistinguishedName();
    }
}
