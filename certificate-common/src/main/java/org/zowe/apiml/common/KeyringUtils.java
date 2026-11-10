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
import java.security.Provider;
import java.security.Security;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utilities for SAF keyring URI handling and KeyStore loading.
 * Used by both {@code certificate-analyser} and {@code zosmf-jwt-check}.
 */
@SuppressWarnings("squid:S106")
public final class KeyringUtils {

    private static final String SECURITY_OVERRIDE_RESOURCE = "jvm.security.override.properties";
    private static final String PROVIDER_PREFIX = "security.provider.";
    private static final Pattern PROVIDER_KEY = Pattern.compile("security\\.provider\\.\\d+");

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
     * @deprecated does not select the protocol for the store type, use {@link #formatKeyringUrl(String, String)}
     */
    @Deprecated(forRemoval = true)
    public static String formatKeyringUrl(String input) {
        return formatKeyringUrl(input, null);
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
     * Converts a keyring URI string to a {@link URL} suitable for opening as a stream.
     *
     * @param uri the keyring URI
     * @return URL object
     * @throws MalformedURLException if the URI is invalid
     * @throws StoresNotInitializeException if the URI does not match keyring format
     * @deprecated does not select the protocol for the store type, use {@link #keyRingUrl(String, String)}
     */
    @Deprecated(forRemoval = true)
    public static URL keyRingUrl(String uri) throws MalformedURLException {
        return keyRingUrl(uri, null);
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
        ensureSecurityProviders();
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(is, pass);
        return keyStore;
    }

    /**
     * Registers the security providers listed in {@code jvm.security.override.properties}, the same file
     * the services pass via {@code -Djava.security.properties}. It is needed when the JVM was started
     * without that override (e.g. the certificate analyser), otherwise keystore types such as
     * {@code JCEHYBRIDRACFKS} are not found. Providers that are already registered or not available
     * in this JVM are skipped. Safe to call repeatedly.
     */
    public static synchronized void ensureSecurityProviders() {
        List<String> names = configuredProviderNames();
        if (names.stream().allMatch(n -> Security.getProvider(n) != null)) return;
        Map<String, Provider> available = new HashMap<>();
        try {
            for (Provider provider : ServiceLoader.load(Provider.class)) {
                available.putIfAbsent(provider.getName(), provider);
            }
        } catch (ServiceConfigurationError | RuntimeException e) {
            System.err.println("WARN: Unable to load security providers: " + e.getMessage());
        }
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            Provider provider = available.get(name);
            if (provider != null && Security.getProvider(name) == null) {
                Security.insertProviderAt(provider, i + 1);
            }
        }
    }

    /**
     * @return provider names from {@code security.provider.N} entries, ordered by N
     */
    static List<String> configuredProviderNames() {
        Properties props = new Properties();
        try (InputStream is = KeyringUtils.class.getResourceAsStream("/" + SECURITY_OVERRIDE_RESOURCE)) {
            if (is == null) return List.of();
            props.load(is);
        } catch (IOException e) {
            System.err.println("WARN: Unable to read " + SECURITY_OVERRIDE_RESOURCE + ": " + e.getMessage());
            return List.of();
        }
        return props.stringPropertyNames().stream()
            .filter(k -> PROVIDER_KEY.matcher(k).matches())
            .sorted(Comparator.comparingInt(k -> Integer.parseInt(k.substring(PROVIDER_PREFIX.length()))))
            .map(k -> props.getProperty(k).trim())
            .toList();
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
