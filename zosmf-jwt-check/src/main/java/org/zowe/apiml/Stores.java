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
            try {
                this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                this.trustStore.load(null, null);
            } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
                System.err.println(e.getMessage());
            }
            return;
        }
        if (isKeyring(conf.getTrustStore())) {
            URL url = keyRingUrl(conf.getTrustStore());
            try (InputStream trustStoreIStream = url.openStream()) {
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
        String formatted = formatKeyringUrl(uri);
        try {
            URL url = new URL(formatted);
            System.out.println("DIAG: new URL('" + formatted + "') succeeded via java.protocol.handler.pkgs");
            return url;
        } catch (MalformedURLException e) {
            System.out.println("DIAG: new URL('" + formatted + "') failed: " + e.getMessage());
            System.out.println("DIAG: Attempting explicit handler fallback...");
            URL fallbackUrl = createUrlWithExplicitHandler(formatted);
            if (fallbackUrl != null) {
                System.out.println("DIAG: Fallback succeeded for '" + formatted + "'");
                return fallbackUrl;
            }
            System.err.println("ERROR: Unknown protocol in '" + formatted + "': " + e.getMessage());
            System.err.println("Ensure the JVM is started with: --add-modules ibm.crypto.zsecurity,ibm.crypto.hdwrcca");
            throw e;
        }
    }

    /**
     * Fallback for IBM Java 17/21+ on z/OS where the safkeyring handler may be
     * registered via the {@code java.net.spi.URLStreamHandlerProvider} SPI or
     * via a legacy {@code <pkg>.<protocol>.Handler} class in modules
     * {@code ibm.crypto.zsecurity} and {@code ibm.crypto.hdwrcca}.
     *
     * <p>Requires the modules to be resolved via {@code --add-modules}.</p>
     */
    private static URL createUrlWithExplicitHandler(String formatted) {
        String protocol = formatted.substring(0, formatted.indexOf(':'));

        // Try 1: SPI URLStreamHandlerProvider classes
        String[] spiProviderClasses = {
            "com.ibm.crypto.zsecurity.provider.safkeyring.Provider",
            "com.ibm.crypto.hdwrCCA.provider.safkeyring.Provider"
        };

        String[] loaderNames = {"SystemCL", "PlatformCL", "ContextCL", "StoresCL"};
        ClassLoader[] loaders = {
            ClassLoader.getSystemClassLoader(),
            ClassLoader.getPlatformClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            Stores.class.getClassLoader()
        };

        System.out.println("DIAG: createUrlWithExplicitHandler: protocol='" + protocol + "'");
        System.out.println("DIAG: Trying SPI providers...");
        for (String providerClassName : spiProviderClasses) {
            for (int i = 0; i < loaders.length; i++) {
                ClassLoader loader = loaders[i];
                if (loader == null) continue;
                try {
                    Class<?> cls = Class.forName(providerClassName, true, loader);
                    java.net.spi.URLStreamHandlerProvider provider =
                        (java.net.spi.URLStreamHandlerProvider) cls.getDeclaredConstructor().newInstance();
                    java.net.URLStreamHandler handler = provider.createURLStreamHandler(protocol);
                    if (handler != null) {
                        System.out.println("DIAG: SUCCESS via SPI: " + providerClassName + " [" + loaderNames[i] + "]");
                        return new URL(null, formatted, handler);
                    } else {
                        System.out.println("DIAG: SPI " + providerClassName + " [" + loaderNames[i] + "] returned null handler");
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("DIAG: SPI " + providerClassName + " [" + loaderNames[i] + "] ClassNotFound");
                } catch (Exception e) {
                    System.out.println("DIAG: SPI " + providerClassName + " [" + loaderNames[i] + "] " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        // Try 2: Legacy Handler classes (e.g. <pkg>.safkeyring.Handler)
        String[] handlerPackages = {
            "com.ibm.crypto.zsecurity.provider",
            "com.ibm.crypto.hdwrCCA.provider"
        };

        System.out.println("DIAG: Trying legacy Handler classes...");
        for (String pkg : handlerPackages) {
            String className = pkg + "." + protocol + ".Handler";
            for (int i = 0; i < loaders.length; i++) {
                ClassLoader loader = loaders[i];
                if (loader == null) continue;
                try {
                    Class<?> cls = Class.forName(className, true, loader);
                    java.net.URLStreamHandler handler = (java.net.URLStreamHandler) cls.getDeclaredConstructor().newInstance();
                    System.out.println("DIAG: SUCCESS via Handler: " + className + " [" + loaderNames[i] + "]");
                    return new URL(null, formatted, handler);
                } catch (ClassNotFoundException e) {
                    System.out.println("DIAG: Handler " + className + " [" + loaderNames[i] + "] ClassNotFound");
                } catch (Exception e) {
                    System.out.println("DIAG: Handler " + className + " [" + loaderNames[i] + "] " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("DIAG: createUrlWithExplicitHandler: ALL attempts failed");
        return null;
    }
}
