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
import java.security.*;

public class VerifierSSLContext {

    private final Stores stores;
    private SSLContext sslContext;

    private VerifierSSLContext(Stores stores) {
        this.stores = stores;
    }

    public Stores getStores() {
        return stores;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    static VerifierSSLContext initSSLContext(Stores stores) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {

        VerifierSSLContext conf = new VerifierSSLContext(stores);
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(stores.getTrustStore());
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(stores.getKeyStore(), stores.getConf().getKeyPasswd().toCharArray());
        conf.sslContext = SSLContext.getInstance("TLSv1.2");
        conf.sslContext.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), new SecureRandom());
        return conf;

    }
}
