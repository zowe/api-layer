/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.common;

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
 * Shared utilities for SAF keyring URI handling and KeyStore loading.
 * Used by both {@code certificate-analyser} and {@code zosmf-jwt-check}.
 */
@SuppressWarnings("squid:S106")
public final class KeyringUtils {

    private static final Pattern KEYRING_PATTERN = Pattern.compile("^(safkeyring[^:]*):/{2,4}([^/]+)/([^/]+)$");

    private KeyringUtils() {
        // utility class
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

    /**
     * Converts a keyring URI string to a {@link URL} suitable for opening as a stream.
     *
     * @param uri the keyring URI
     * @return URL object
     * @throws MalformedURLException if the URI is invalid
     * @throws StoresNotInitializeException if the URI does not match keyring format
     */
    public static URL keyRingUrl(String uri) throws MalformedURLException {
        if (!isKeyring(uri)) {
            throw new StoresNotInitializeException("Incorrect key ring format: " + uri
                + ". Make sure you use format safkeyring://userId/keyRing");
        }
        String formatted = formatKeyringUrl(uri);
        try {
            return new URL(formatted);
        } catch (MalformedURLException e) {
            System.err.println("ERROR: Unknown protocol in '" + formatted + "': " + e.getMessage());
            System.err.println("Ensure the JVM is started with: --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca");
            System.err.println("And that ensureSafkeyringHandler() has been called to set java.protocol.handler.pkgs");
            throw e;
        }
    }

    /**
     * Loads a {@link KeyStore} from an input stream.
     *
     * @param is   input stream to read from
     * @param pass keystore password
     * @param type keystore type (e.g. PKCS12, JKS)
     * @return loaded KeyStore instance
     */
    public static KeyStore readKeyStore(InputStream is, char[] pass, String type) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(is, pass);
        return keyStore;
    }

    /**
     * Registers IBM SAF keyring URL protocol handler packages via the
     * {@code java.protocol.handler.pkgs} system property.
     *
     * <p>On IBM Java 17/21 (z/OS), this property works in conjunction with
     * {@code --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca} to enable
     * the {@code safkeyring://} URL protocol. The {@code --add-modules} flag
     * resolves the module (making classes accessible), while this property tells
     * the {@link java.net.URL} class which packages to search for the handler.</p>
     */
    public static void ensureSafkeyringHandler() {
        String[] packagePrefixes = {
            "com.ibm.crypto.zsecurity.provider",
            "com.ibm.crypto.hdwrCCA.provider"
        };
        String existing = System.getProperty("java.protocol.handler.pkgs", "");
        StringBuilder sb = new StringBuilder(existing);
        for (String prefix : packagePrefixes) {
            if (!existing.contains(prefix)) {
                if (!sb.isEmpty()) {
                    sb.append('|');
                }
                sb.append(prefix);
            }
        }
        System.setProperty("java.protocol.handler.pkgs", sb.toString());
    }
}
