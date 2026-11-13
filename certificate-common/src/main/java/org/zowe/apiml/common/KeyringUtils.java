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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utilities for SAF keyring URI handling and KeyStore loading.
 * Used by both {@code certificate-analyser} and {@code zosmf-jwt-check}.
 */
@SuppressWarnings("squid:S106")
public final class KeyringUtils {

    private static final String PROVIDER_HINT = "For JCECCARACFKS and JCEHYBRIDRACFKS keyrings the matching IBM security provider must be registered:"
        + " start the JVM with -Djava.security.properties=<apiml-common-lib>/bin/jvm.security.override.properties"
        + " and add the module providing the provider to --add-modules (list them with: java --list-modules | grep ibm.crypto).";

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
     * Normalizes a keyring URI and selects the protocol for the configured store type.
     * Explicit provider-specific protocols are preserved.
     *
     * @param input raw keyring URI
     * @param type keystore or truststore type
     * @return normalized URI, or the original input if not a keyring
     */
    public static String formatKeyringUrl(String input, String type) {
        if (input == null) return null;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        if (matcher.matches()) {
            String protocol = matcher.group(1);
            if ("safkeyring".equals(protocol) && type != null) {
                // the protocol handler of the security provider that implements the store type
                protocol = switch (type.toUpperCase(Locale.ROOT)) {
                    case "JCERACFKS" -> "safkeyringjce";
                    case "JCECCARACFKS" -> "safkeyringjcecca";
                    case "JCEHYBRIDRACFKS" -> "safkeyringjcehybrid";
                    default -> protocol;
                };
            }
            return protocol + "://" + matcher.group(2) + "/" + matcher.group(3);
        }
        return input;
    }

    /**
     * Converts a keyring URI to a URL using the configured store type's protocol.
     *
     * @param uri the keyring URI
     * @param type keystore or truststore type
     * @return URL object
     * @throws MalformedURLException if the protocol handler is unavailable
     * @throws StoresNotInitializeException if the URI does not match keyring format
     */
    public static URL keyRingUrl(String uri, String type) throws MalformedURLException {
        if (!isKeyring(uri)) {
            throw new StoresNotInitializeException("Incorrect key ring format: " + uri
                + ". Make sure you use format safkeyring://userId/keyRing");
        }
        String formatted = formatKeyringUrl(uri, type);
        try {
            return new URL(formatted);
        } catch (MalformedURLException e) {
            System.err.println("ERROR: Unknown protocol in '" + formatted + "': " + e.getMessage());
            System.err.println("Ensure the JVM is started with: -Djava.protocol.handler.pkgs=com.ibm.crypto.provider"
                + " --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca");
            System.err.println(PROVIDER_HINT);
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
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(type);
        } catch (KeyStoreException e) {
            if (type.toUpperCase(Locale.ROOT).endsWith("RACFKS")) {
                throw new KeyStoreException(e.getMessage() + ". " + PROVIDER_HINT, e);
            }
            throw e;
        }
        keyStore.load(is, pass);
        return keyStore;
    }
}
