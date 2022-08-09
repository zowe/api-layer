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

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SSLContextFactory {

    private final Stores stores;
    private SSLContext sslContext;
    private SSLContext sslContextWithKeystore;

    private SSLContextFactory(Stores stores) {
        this.stores = stores;
    }

    public Stores getStores() {
        return stores;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public SSLContext getSslContextWithKeystore() {
        return sslContextWithKeystore;
    }

    static SSLContextFactory initSSLContextWithKeystore(Stores stores) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {

        SSLContextFactory conf = new SSLContextFactory(stores);
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(stores.getTrustStore());
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(stores.getKeyStore(), stores.getConf().getKeyPasswd().toCharArray());
        conf.sslContextWithKeystore = SSLContext.getInstance("TLSv1.2");
        X509KeyManager originalKm = (X509KeyManager) keyFactory.getKeyManagers()[0];
        X509KeyManager km = new X509KeyManager() {
            public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
                if (stores.getConf().getKeyAlias() == null) {
                    return originalKm.chooseClientAlias(keyType, issuers, socket);
                }
                return stores.getConf().getKeyAlias();
            }

            public X509Certificate[] getCertificateChain(String alias) {
                return originalKm.getCertificateChain(alias);
            }

            @Override
            public String[] getClientAliases(String s, Principal[] principals) {
                return originalKm.getClientAliases(s, principals);
            }

            @Override
            public String[] getServerAliases(String s, Principal[] principals) {
                return originalKm.getServerAliases(s, principals);
            }

            @Override
            public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                if (stores.getConf().getKeyAlias() == null) {
                    return originalKm.chooseServerAlias(s, principals, socket);
                }
                return stores.getConf().getKeyAlias();
            }

            @Override
            public PrivateKey getPrivateKey(String s) {
                return originalKm.getPrivateKey(s);
            }
        };
        conf.sslContextWithKeystore.init(new KeyManager[]{km}, trustFactory.getTrustManagers(), new SecureRandom());
        return conf;

    }

    static SSLContextFactory initSSLContextWithoutKeystore(Stores stores) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {

        SSLContextFactory conf = new SSLContextFactory(stores);
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(stores.getTrustStore());
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        emptyKeystore.load(null, null);
        keyFactory.init(emptyKeystore, null);
        conf.sslContext = SSLContext.getInstance("TLSv1.2");
        conf.sslContext.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), new SecureRandom());
        return conf;

    }
}
