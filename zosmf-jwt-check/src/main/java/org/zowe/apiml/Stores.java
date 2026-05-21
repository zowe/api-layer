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
import java.security.cert.CertificateException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads Java {@link java.security.KeyStore} instances from the filesystem
 * or z/OS SAF keyrings. Supports PKCS12, JKS, and {@code safkeyring://} URIs.
 */
@SuppressWarnings("squid:S106")
public class Stores {

    private static final Pattern KEYRING_PATTERN = Pattern.compile("^(safkeyring[^:]*):/{2,4}([^/]+)/([^/]+)$");

    private KeyStore keyStore;
    private KeyStore trustStore;
    private final ZosmfJwtCheckConfig conf;

    public Stores(ZosmfJwtCheckConfig conf) {
        this.conf = conf;
        init();
    }

    /**
     * Checks whether the given path is a SAF keyring URI.
     *
     * @param input store path to check
     * @return {@code true} if the path matches the keyring pattern
     */
    public static boolean isKeyring(String input) {
        if (input == null) return false;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        return matcher.matches();
    }

    /**
     * Normalizes a keyring URI to the canonical {@code safkeyring://userId/keyRing} format.
     *
     * @param input raw keyring URI
     * @return normalized URI, or the original input if not a keyring
     */
    public static String formatKeyringUrl(String input) {
        if (input == null) return null;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1) + "://" + matcher.group(2) + "/" + matcher.group(3);
        }
        return input;
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
            System.out.println("No truststore specified, will use empty.");
            try {
                this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                this.trustStore.load(null, null);
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                System.err.println(e.getMessage());
            }
            return;
        }
        if (isKeyring(conf.getTrustStore())) {
            try (InputStream trustStoreIStream = keyRingUrl(conf.getTrustStore()).openStream()) {
                this.trustStore = readKeyStore(trustStoreIStream, conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
            }
        } else {
            try (InputStream trustStoreIStream = new FileInputStream(conf.getTrustStore())) {
                this.trustStore = readKeyStore(trustStoreIStream, conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
            }
        }
    }

    private void initKeystore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (conf.getKeyStore() == null) {
            return;
        }
        if (isKeyring(conf.getKeyStore())) {
            try (InputStream keyringIStream = keyRingUrl(conf.getKeyStore()).openStream()) {
                this.keyStore = readKeyStore(keyringIStream, conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
                this.trustStore = this.keyStore;
            } catch (Exception e) {
                throw new StoresNotInitializeException(e.getMessage());
            }
        } else {
            try (InputStream keyStoreIStream = new FileInputStream(conf.getKeyStore())) {
                this.keyStore = readKeyStore(keyStoreIStream, conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
            }
        }
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

    public ZosmfJwtCheckConfig getConf() {
        return conf;
    }

    public static URL keyRingUrl(String uri) throws MalformedURLException {
        if (!isKeyring(uri)) {
            throw new StoresNotInitializeException("Incorrect key ring format: " + uri
                + ". Make sure you use format safkeyring://userId/keyRing");
        }
        return new URL(formatKeyringUrl(uri));
    }
}
