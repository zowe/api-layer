/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.Collection;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class SSLContextBuilder extends org.apache.hc.core5.ssl.SSLContextBuilder {

    @Getter(AccessLevel.PACKAGE)
    private Collection<TrustManager> trustManagers;

    @Override
    protected void initSSLContext(SSLContext sslContext, Collection<KeyManager> keyManagers,
            Collection<TrustManager> trustManagers, SecureRandom secureRandom) throws KeyManagementException {
        super.initSSLContext(sslContext, keyManagers, trustManagers, secureRandom);
        this.trustManagers = trustManagers;
    }

    public static SSLContextBuilder create() {
        return new SSLContextBuilder();
    }

}
