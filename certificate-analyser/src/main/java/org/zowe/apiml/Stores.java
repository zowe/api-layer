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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Stores {

    public static final String SAFKEYRING = "safkeyring";
    private KeyStore keyStore;
    private KeyStore trustStore;
    private final Config conf;
    private Map<String, Certificate> caList;

    public Stores(Config conf) {
        this.conf = conf;
        init();
    }

    void init() {
        try {
            initKeystore();
            if (trustStore == null) {
                initTruststore();
            }
        } catch (FileNotFoundException e) {
            throw new StoresNotInitializeException("Error while loading keystore file. Error message: " + e.getMessage() + "\n" +
                "Possible solution: Verify correct path to the keystore. Change owner or permission to the keystore file.");
        } catch (Exception e) {
            throw new StoresNotInitializeException(e.getMessage());
        }
    }

    private void initTruststore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (conf.getTrustStore() == null) {
            System.out.println("No keystore specified, will use empty.");
            try {
                this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            } catch (KeyStoreException e) {
                System.err.println(e.getMessage());
            }
            return;
        }
        try (InputStream trustStoreIStream = new FileInputStream(conf.getTrustStore())) {
            this.trustStore = readKeyStore(trustStoreIStream, conf.getTrustPasswd().toCharArray(), conf.getTrustStoreType());
        }

    }

    private void initKeystore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (conf.getKeyStore() == null) {
            System.out.println("No keystore specified, will use empty.");
            try {
                this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            } catch (KeyStoreException e) {
                System.err.println(e.getMessage());
            }
            return;
        }
        if (conf.getKeyStore().startsWith(SAFKEYRING)) {
            try (InputStream keyringIStream = keyRingUrl(conf.getKeyStore()).openStream()) {
                this.keyStore = readKeyStore(keyringIStream, conf.getKeyPasswd().toCharArray(), conf.getKeyStoreType());
                this.trustStore = this.keyStore;
            } catch (Exception e) {
                throw new StoresNotInitializeException(e.getMessage());
            }
        } else {
            try (InputStream keyStoreIStream = new FileInputStream(conf.getKeyStore())) {
                this.keyStore = readKeyStore(keyStoreIStream, conf.getKeyPasswd().toCharArray(), conf.getKeyStoreType());
            }
        }
    }

    public Map<String, Certificate> getListOfCertificates() throws KeyStoreException {
        if (this.caList != null) {
            return this.caList;
        }
        this.caList = new HashMap<>();
        Enumeration<String> aliases = trustStore.aliases();

        while (aliases.hasMoreElements()) {
            String certAuthAlias = aliases.nextElement();
            this.caList.put(certAuthAlias, trustStore.getCertificate(certAuthAlias));
        }
        return this.caList;
    }

    public X509Certificate getX509Certificate(String alias) throws KeyStoreException {
        Certificate[] certificate = getServerCertificateChain(alias);
        if (certificate.length > 0) {
            return (X509Certificate) certificate[0];
        } else {
            System.out.println("Alias \"" + alias + "\" is not available in keystore.");
            throw new StoresNotInitializeException("No x509 certificate available in keystore");
        }
    }

    public Certificate[] getServerCertificateChain(String alias) throws KeyStoreException {
        if (alias == null) {
            alias = keyStore.aliases().nextElement();
        }
        return keyStore.getCertificateChain(alias);
    }

    public static KeyStore readKeyStore(InputStream is, char[] pass, String type) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(is, pass);
        return keyStore;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public Config getConf() {
        return conf;
    }

    public static URL keyRingUrl(String uri) throws MalformedURLException {
        if (!uri.startsWith(SAFKEYRING + ":////")) {
            throw new StoresNotInitializeException("Incorrect key ring format: " + uri
                + ". Make sure you use format safkeyring:////userId/keyRing");
        }

        return new URL(replaceFourSlashes(uri));

    }

    public static String replaceFourSlashes(String storeUri) {
        return storeUri == null ? null : storeUri.replaceFirst("////", "//");
    }
}
