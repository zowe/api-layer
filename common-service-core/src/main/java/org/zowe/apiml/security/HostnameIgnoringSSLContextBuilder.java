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
public class HostnameIgnoringSSLContextBuilder extends org.apache.hc.core5.ssl.SSLContextBuilder {

    @Override
    protected void initSSLContext(SSLContext sslContext, Collection<KeyManager> keyManagers,
            Collection<TrustManager> trustManagers, SecureRandom secureRandom) throws KeyManagementException {

        var tm = trustManagers.iterator().next();
        Collection<TrustManager> laxTrustManager = new ArrayList<>();
        laxTrustManager.add(new HostnameIgnoringTrustManager((X509TrustManager) tm));
        super.initSSLContext(sslContext, keyManagers, laxTrustManager, secureRandom);
    }

    public static HostnameIgnoringSSLContextBuilder create() {
        return new HostnameIgnoringSSLContextBuilder();
    }

}
