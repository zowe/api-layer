/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

public class SSLContextFactory {

    private final Stores stores;
    private SSLContext sslContext;

    private SSLContextFactory(Stores stores) {
        this.stores = stores;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public static SSLContextFactory initSSLContext(Stores stores) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, CertificateException, IOException {
        SSLContextFactory factory = new SSLContextFactory(stores);

        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(stores.getTrustStore());

        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        if (stores.getKeyStore() != null) {
            keyFactory.init(stores.getKeyStore(), stores.getConf().getKeyStorePassword().toCharArray());
        } else {
            KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyKeystore.load(null, null);
            keyFactory.init(emptyKeystore, null);
        }

        factory.sslContext = SSLContext.getInstance("TLSv1.2");
        factory.sslContext.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), new SecureRandom());
        return factory;
    }
}
