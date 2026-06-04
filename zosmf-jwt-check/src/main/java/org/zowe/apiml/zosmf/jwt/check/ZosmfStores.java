/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zosmf.jwt.check;

import org.zowe.apiml.common.KeyringUtils;
import org.zowe.apiml.common.StoresNotInitializeException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Loads Java {@link java.security.KeyStore} instances from the filesystem
 * or z/OS SAF keyrings. Supports PKCS12, JKS, and {@code safkeyring://} URIs.
 */
@SuppressWarnings("squid:S106")
public class ZosmfStores {

    private KeyStore keyStore;
    private KeyStore trustStore;
    private final ZosmfJwtCheckConfig conf;

    public ZosmfStores(ZosmfJwtCheckConfig conf) {
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
            try {
                this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                this.trustStore.load(null, null);
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                System.err.println(e.getMessage());
            }
            return;
        }
        if (KeyringUtils.isKeyring(conf.getTrustStore())) {
            URL url = KeyringUtils.keyRingUrl(conf.getTrustStore());
            try (InputStream trustStoreIStream = url.openStream()) {
                this.trustStore = KeyringUtils.readKeyStore(trustStoreIStream, conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
            }
        } else {
            try (InputStream trustStoreIStream = new FileInputStream(conf.getTrustStore())) {
                this.trustStore = KeyringUtils.readKeyStore(trustStoreIStream, conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
            }
        }
    }

    private void initKeystore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (conf.getKeyStore() == null) {
            return;
        }
        if (KeyringUtils.isKeyring(conf.getKeyStore())) {
            try (InputStream keyringIStream = KeyringUtils.keyRingUrl(conf.getKeyStore()).openStream()) {
                this.keyStore = KeyringUtils.readKeyStore(keyringIStream, conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
                this.trustStore = this.keyStore;
            } catch (Exception e) {
                throw new StoresNotInitializeException(e.getMessage());
            }
        } else {
            try (InputStream keyStoreIStream = new FileInputStream(conf.getKeyStore())) {
                this.keyStore = KeyringUtils.readKeyStore(keyStoreIStream, conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
            }
        }
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public ZosmfJwtCheckConfig getConf() {
        return conf;
    }
}
