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
            System.out.println("DEBUG [Stores] keyRingUrl: Standard URL creation failed, trying explicit handler lookup...");
            URL fallbackUrl = createUrlWithExplicitHandler(formatted);
            if (fallbackUrl != null) {
                System.out.println("DEBUG [Stores] keyRingUrl: URL created via explicit handler. protocol=" + fallbackUrl.getProtocol());
                return fallbackUrl;
            }
            System.err.println("DEBUG [Stores] keyRingUrl: MalformedURLException for '" + formatted + "': " + e.getMessage());
            System.err.println("DEBUG [Stores] keyRingUrl: java.protocol.handler.pkgs=" + System.getProperty("java.protocol.handler.pkgs", "<not set>"));
            throw e;
        }
    }

    /**
     * Fallback for IBM Java 17/21+ on z/OS where the safkeyring handler is registered
     * via the {@code java.net.spi.URLStreamHandlerProvider} SPI mechanism in modules
     * {@code ibm.crypto.zsecurity} and {@code ibm.crypto.hdwrcca}.
     *
     * <p>If the modules are resolved (via {@code --add-modules}), the standard
     * {@code new URL()} works. This fallback handles the case where the module IS
     * resolved but the ServiceLoader lookup failed, or where we can reflectively
     * instantiate the provider and obtain a handler.</p>
     */
    private static URL createUrlWithExplicitHandler(String formatted) {
        // Extract protocol from the formatted URI (e.g., "safkeyring", "safkeyringjce", etc.)
        String protocol = formatted.substring(0, formatted.indexOf(':'));
        System.out.println("DEBUG [Stores] createUrlWithExplicitHandler: protocol='" + protocol + "' formatted='" + formatted + "'");

        // Try 1: Use the URLStreamHandlerProvider SPI classes directly (Java 17/21 on z/OS)
        // These are the actual provider classes found in ibm.crypto.zsecurity and ibm.crypto.hdwrcca
        String[] spiProviderClasses = {
            "com.ibm.crypto.zsecurity.provider.safkeyring.Provider",
            "com.ibm.crypto.hdwrCCA.provider.safkeyring.Provider"
        };

        ClassLoader[] loaders = {
            ClassLoader.getSystemClassLoader(),
            ClassLoader.getPlatformClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            Stores.class.getClassLoader()
        };
        String[] loaderNames = {"SystemClassLoader", "PlatformClassLoader", "ContextClassLoader", "StoresClassLoader"};

        System.out.println("DEBUG [Stores] createUrlWithExplicitHandler: Trying SPI providers...");
        for (String providerClassName : spiProviderClasses) {
            for (int i = 0; i < loaders.length; i++) {
                ClassLoader loader = loaders[i];
                if (loader == null) {
                    System.out.println("DEBUG [Stores]   Skipping null loader: " + loaderNames[i]);
                    continue;
                }
                try {
                    System.out.println("DEBUG [Stores]   Trying SPI class=" + providerClassName + " loader=" + loaderNames[i] + " (" + loader.getClass().getName() + ")");
                    Class<?> cls = Class.forName(providerClassName, true, loader);
                    System.out.println("DEBUG [Stores]   Class loaded successfully: " + cls.getName());
                    java.net.spi.URLStreamHandlerProvider provider =
                        (java.net.spi.URLStreamHandlerProvider) cls.getDeclaredConstructor().newInstance();
                    System.out.println("DEBUG [Stores]   Provider instantiated: " + provider.getClass().getName());
                    java.net.URLStreamHandler handler = provider.createURLStreamHandler(protocol);
                    if (handler != null) {
                        System.out.println("DEBUG [Stores]   SUCCESS: Handler obtained for protocol '" + protocol + "' from " + providerClassName + " via " + loaderNames[i]);
                        return new URL(null, formatted, handler);
                    } else {
                        System.out.println("DEBUG [Stores]   Provider returned null handler for protocol '" + protocol + "' (not supported by this provider)");
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("DEBUG [Stores]   ClassNotFoundException: " + providerClassName + " not found via " + loaderNames[i]);
                } catch (Exception e) {
                    System.out.println("DEBUG [Stores]   Exception: " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }

        // Try 2: Legacy approach - look for Handler class directly (Java 8 style)
        System.out.println("DEBUG [Stores] createUrlWithExplicitHandler: SPI providers not found. Trying legacy Handler classes...");
        String[] handlerPackages = {
            "com.ibm.crypto.provider",
            "com.ibm.crypto.zsecurity.provider",
            "com.ibm.crypto.hdwrCCA.provider"
        };

        for (String pkg : handlerPackages) {
            String className = pkg + "." + protocol + ".Handler";
            for (int i = 0; i < loaders.length; i++) {
                ClassLoader loader = loaders[i];
                if (loader == null) continue;
                try {
                    System.out.println("DEBUG [Stores]   Trying Handler class=" + className + " loader=" + loaderNames[i]);
                    Class<?> cls = Class.forName(className, true, loader);
                    java.net.URLStreamHandler handler = (java.net.URLStreamHandler) cls.getDeclaredConstructor().newInstance();
                    System.out.println("DEBUG [Stores]   SUCCESS: Handler instantiated: " + className + " via " + loaderNames[i]);
                    return new URL(null, formatted, handler);
                } catch (ClassNotFoundException e) {
                    System.out.println("DEBUG [Stores]   ClassNotFoundException: " + className + " not found via " + loaderNames[i]);
                } catch (Exception e) {
                    System.out.println("DEBUG [Stores]   Exception: " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }

        System.err.println("DEBUG [Stores] createUrlWithExplicitHandler: FAILED - Could not find handler for protocol '" + protocol + "' in any known location.");
        System.err.println("DEBUG [Stores] createUrlWithExplicitHandler: Ensure the JVM is started with: --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca");
        System.err.println("DEBUG [Stores] createUrlWithExplicitHandler: Resolved modules can be checked with: java --show-module-resolution --add-modules ibm.crypto.zsecurity -version");
        return null;
    }
}
