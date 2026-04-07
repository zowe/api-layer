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
import lombok.NoArgsConstructor;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder meant to be used only for non-strict configuration
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class HostnameIgnoringSSLContextBuilder extends ApimlSSLContextBuilder {

    @Override
    protected void initSSLContext(SSLContext sslContext, Collection<KeyManager> keyManagers,
            Collection<TrustManager> trustManagers, SecureRandom secureRandom) throws KeyManagementException {

        Collection<TrustManager> laxTrustManager = new ArrayList<>();
        if (trustManagers != null) {
            trustManagers.forEach(tm -> {
                if (tm instanceof X509TrustManager x509tm) {
                    laxTrustManager.add(new HostnameIgnoringTrustManager(x509tm));
                } else {
                    laxTrustManager.add(tm);
                }
            });
        }

        super.initSSLContext(sslContext, keyManagers, laxTrustManager, secureRandom);
    }

    public static HostnameIgnoringSSLContextBuilder create() {
        return new HostnameIgnoringSSLContextBuilder();
    }

}
