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
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Builds a TLSv1.2 {@link SSLContext} in two modes:
 * <ul>
 *   <li>{@link #initSSLContext(Stores)} — normal mode using real truststore/keystore</li>
 *   <li>{@link #initTrustAllSSLContext()} — trust-all mode for DISABLED verification</li>
 * </ul>
 */
// TODO: REMOVE all "DEBUG [SSLContextFactory]" logging lines after SAF keyring issue is resolved
@SuppressWarnings("squid:S106")
public class SSLContextFactory {

    private final Stores stores;
    private SSLContext sslContext;

    private SSLContextFactory(Stores stores) {
        this.stores = stores;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * Creates an SSLContext using the provided keystore and truststore.
     *
     * @param stores loaded keystore/truststore pair
     * @return factory holding the initialized SSLContext
     */
    public static SSLContextFactory initSSLContext(Stores stores) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException, CertificateException, IOException {
        System.out.println("DEBUG [SSLContextFactory] initSSLContext() called");
        SSLContextFactory factory = new SSLContextFactory(stores);

        System.out.println("DEBUG [SSLContextFactory] Initializing TrustManagerFactory with trustStore (type=" + stores.getTrustStore().getType() + ", size=" + stores.getTrustStore().size() + ")");
        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(stores.getTrustStore());

        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        if (stores.getKeyStore() != null) {
            System.out.println("DEBUG [SSLContextFactory] Initializing KeyManagerFactory with keyStore (type=" + stores.getKeyStore().getType() + ", size=" + stores.getKeyStore().size() + ")");
            keyFactory.init(stores.getKeyStore(), stores.getConf().getKeyStorePassword().toCharArray());
        } else {
            System.out.println("DEBUG [SSLContextFactory] No keyStore, using empty keystore for KeyManagerFactory");
            KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyKeystore.load(null, null);
            keyFactory.init(emptyKeystore, null);
        }

        factory.sslContext = SSLContext.getInstance("TLSv1.2");
        factory.sslContext.init(keyFactory.getKeyManagers(), trustFactory.getTrustManagers(), new SecureRandom());
        System.out.println("DEBUG [SSLContextFactory] SSLContext created successfully (protocol=" + factory.sslContext.getProtocol() + ")");
        return factory;
    }

    /**
     * Creates an SSLContext that trusts all certificates. Use only when verification is DISABLED.
     *
     * @return factory holding the trust-all SSLContext
     */
    public static SSLContextFactory initTrustAllSSLContext() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        SSLContextFactory factory = new SSLContextFactory(null);

        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    // trust all
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    // trust all
                }
            }
        };

        KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        emptyKeystore.load(null, null);
        KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyFactory.init(emptyKeystore, null);

        factory.sslContext = SSLContext.getInstance("TLSv1.2");
        factory.sslContext.init(keyFactory.getKeyManagers(), trustAllCerts, new SecureRandom());
        System.out.println("WARNING: SSL certificate verification is DISABLED. All certificates will be trusted.");
        return factory;
    }
}
