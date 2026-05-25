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
// TODO: REMOVE all "DEBUG [Stores]" logging lines after SAF keyring issue is resolved
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
        System.out.println("DEBUG [Stores] init() called");
        System.out.println("DEBUG [Stores]   trustStore path=" + conf.getTrustStore());
        System.out.println("DEBUG [Stores]   trustStore type=" + conf.getTrustStoreType());
        System.out.println("DEBUG [Stores]   trustStore isKeyring=" + isKeyring(conf.getTrustStore()));
        System.out.println("DEBUG [Stores]   keyStore path=" + conf.getKeyStore());
        System.out.println("DEBUG [Stores]   keyStore type=" + (conf.getKeyStore() != null ? conf.getKeyStoreType() : "<n/a>"));
        System.out.println("DEBUG [Stores]   keyStore isKeyring=" + isKeyring(conf.getKeyStore()));
        System.out.println("DEBUG [Stores]   java.protocol.handler.pkgs=" + System.getProperty("java.protocol.handler.pkgs", "<not set>"));
        try {
            System.out.println("DEBUG [Stores] Calling initKeystore()...");
            initKeystore();
            System.out.println("DEBUG [Stores] initKeystore() completed. trustStore set by keystore=" + (trustStore != null));
            if (trustStore == null) {
                System.out.println("DEBUG [Stores] Calling initTruststore()...");
                initTruststore();
                System.out.println("DEBUG [Stores] initTruststore() completed successfully.");
            }
        } catch (FileNotFoundException e) {
            System.err.println("DEBUG [Stores] FileNotFoundException in init(): " + e.getMessage());
            e.printStackTrace(System.err);
            throw new StoresNotInitializeException("Error while loading keystore file. Error message: " + e.getMessage() + "\n" +
                "Possible solution: Verify correct path to the keystore. Change owner or permission to the keystore file.");
        } catch (Exception e) {
            System.err.println("DEBUG [Stores] Exception in init(): " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new StoresNotInitializeException(e.getMessage());
        }
    }

    private void initTruststore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (conf.getTrustStore() == null) {
            System.out.println("DEBUG [Stores] initTruststore: No truststore specified, will use empty.");
            try {
                this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                this.trustStore.load(null, null);
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                System.err.println(e.getMessage());
            }
            return;
        }
        if (isKeyring(conf.getTrustStore())) {
            System.out.println("DEBUG [Stores] initTruststore: Detected SAF keyring URI: " + conf.getTrustStore());
            String formatted = formatKeyringUrl(conf.getTrustStore());
            System.out.println("DEBUG [Stores] initTruststore: Formatted keyring URL: " + formatted);
            System.out.println("DEBUG [Stores] initTruststore: Calling keyRingUrl()...");
            URL url = keyRingUrl(conf.getTrustStore());
            System.out.println("DEBUG [Stores] initTruststore: URL object created: " + url + " (protocol=" + url.getProtocol() + ")");
            System.out.println("DEBUG [Stores] initTruststore: Calling url.openStream()...");
            try (InputStream trustStoreIStream = url.openStream()) {
                System.out.println("DEBUG [Stores] initTruststore: openStream() succeeded. Loading keystore type=" + conf.getTrustStoreType());
                this.trustStore = readKeyStore(trustStoreIStream, conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
                System.out.println("DEBUG [Stores] initTruststore: Truststore loaded from keyring. Aliases count=" + trustStore.size());
            }
        } else {
            System.out.println("DEBUG [Stores] initTruststore: Loading from file: " + conf.getTrustStore());
            try (InputStream trustStoreIStream = new FileInputStream(conf.getTrustStore())) {
                this.trustStore = readKeyStore(trustStoreIStream, conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
                System.out.println("DEBUG [Stores] initTruststore: Truststore loaded from file. Aliases count=" + trustStore.size());
            }
        }
    }

    private void initKeystore() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        if (conf.getKeyStore() == null) {
            System.out.println("DEBUG [Stores] initKeystore: No keystore specified, skipping.");
            return;
        }
        if (isKeyring(conf.getKeyStore())) {
            System.out.println("DEBUG [Stores] initKeystore: Detected SAF keyring URI: " + conf.getKeyStore());
            try (InputStream keyringIStream = keyRingUrl(conf.getKeyStore()).openStream()) {
                this.keyStore = readKeyStore(keyringIStream, conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
                this.trustStore = this.keyStore;
                System.out.println("DEBUG [Stores] initKeystore: Keystore loaded from keyring. Aliases count=" + keyStore.size());
            } catch (Exception e) {
                System.err.println("DEBUG [Stores] initKeystore: Exception loading keyring: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
                throw new StoresNotInitializeException(e.getMessage());
            }
        } else {
            System.out.println("DEBUG [Stores] initKeystore: Loading from file: " + conf.getKeyStore());
            try (InputStream keyStoreIStream = new FileInputStream(conf.getKeyStore())) {
                this.keyStore = readKeyStore(keyStoreIStream, conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
                System.out.println("DEBUG [Stores] initKeystore: Keystore loaded from file. Aliases count=" + keyStore.size());
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
        String formatted = formatKeyringUrl(uri);
        System.out.println("DEBUG [Stores] keyRingUrl: Creating URL from: " + formatted);
        try {
            URL url = new URL(formatted);
            System.out.println("DEBUG [Stores] keyRingUrl: URL created successfully. protocol=" + url.getProtocol()
                + " host=" + url.getHost() + " path=" + url.getPath());
            return url;
        } catch (MalformedURLException e) {
            System.err.println("DEBUG [Stores] keyRingUrl: MalformedURLException for '" + formatted + "': " + e.getMessage());
            System.err.println("DEBUG [Stores] keyRingUrl: java.protocol.handler.pkgs=" + System.getProperty("java.protocol.handler.pkgs", "<not set>"));
            throw e;
        }
    }
}
