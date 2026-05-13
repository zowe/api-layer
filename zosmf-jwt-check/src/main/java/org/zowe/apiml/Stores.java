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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
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
            System.out.println("DEBUG [Stores] initTruststore: Loading keyring truststore (multi-strategy)...");
            this.trustStore = loadKeyringKeyStore(conf.getTrustStore(), conf.getTrustStorePassword().toCharArray(), conf.getTrustStoreType());
            System.out.println("DEBUG [Stores] initTruststore: Truststore loaded from keyring. Aliases count=" + trustStore.size());
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
            try {
                this.keyStore = loadKeyringKeyStore(conf.getKeyStore(), conf.getKeyStorePassword().toCharArray(), conf.getKeyStoreType());
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

    /**
     * Loads a KeyStore from a SAF keyring using multiple strategies in order:
     * <ol>
     *   <li>Standard URL approach: {@code new URL("safkeyring://...").openStream()}</li>
     *   <li>RACFInputStream via reflection (bypasses URL handler)</li>
     *   <li>Direct JCERACFKS load via IBMZSecurity provider (no InputStream needed)</li>
     * </ol>
     */
    // TODO: REMOVE debug logging after SAF keyring issue is resolved
    @SuppressWarnings("squid:S3011")
    static KeyStore loadKeyringKeyStore(String uri, char[] password, String type) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        String formatted = formatKeyringUrl(uri);
        System.out.println("DEBUG [Stores] loadKeyringKeyStore: uri='" + uri + "' formatted='" + formatted + "' type=" + type);
        System.out.println("DEBUG [Stores] loadKeyringKeyStore: java.protocol.handler.pkgs="
            + System.getProperty("java.protocol.handler.pkgs", "<not set>"));

        Matcher matcher = KEYRING_PATTERN.matcher(uri);
        if (!matcher.matches()) {
            matcher = KEYRING_PATTERN.matcher(formatted);
        }
        String userId = matcher.matches() ? matcher.group(2) : null;
        String ringName = matcher.matches() ? matcher.group(3) : null;

        // Strategy 1: Standard URL-based approach (same as main API ML services)
        try {
            System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 1 - URL approach...");
            URL url = new URL(formatted);
            System.out.println("DEBUG [Stores] loadKeyringKeyStore: URL created. protocol=" + url.getProtocol());
            try (InputStream is = url.openStream()) {
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: URL.openStream() SUCCEEDED (stream class=" + is.getClass().getName() + ")");
                KeyStore ks = KeyStore.getInstance(type);
                ks.load(is, password);
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 1 SUCCEEDED. Aliases count=" + ks.size());
                return ks;
            }
        } catch (MalformedURLException urlEx) {
            System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 1 FAILED (MalformedURLException): " + urlEx.getMessage());
        } catch (IOException ioEx) {
            System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 1 FAILED (IOException on openStream): "
                + ioEx.getClass().getName() + ": " + ioEx.getMessage());
        }

        // Strategy 2: RACFInputStream via reflection
        if (userId != null && ringName != null) {
            try {
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 2 - RACFInputStream('" + userId + "', '" + ringName + "')...");
                Class<?> racfClass = Class.forName("com.ibm.crypto.zsecurity.provider.RACFInputStream");
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: RACFInputStream class loaded (classLoader=" + racfClass.getClassLoader() + ")");
                Constructor<?> ctor = racfClass.getConstructor(String.class, String.class, char[].class);
                InputStream is = (InputStream) ctor.newInstance(userId, ringName, password);
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: RACFInputStream created. Loading KeyStore...");
                KeyStore ks = KeyStore.getInstance(type);
                ks.load(is, password);
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 2 SUCCEEDED. Aliases count=" + ks.size());
                return ks;
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                String causeMsg = cause != null ? cause.getMessage() : ite.getMessage();
                System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 2 FAILED (RACFInputStream constructor threw): " + causeMsg);
                if (cause != null) {
                    cause.printStackTrace(System.err);
                }
                // If the error is about private key access, try Strategy 3
                if (cause instanceof IOException && causeMsg != null && causeMsg.contains("private key")) {
                    System.out.println("DEBUG [Stores] loadKeyringKeyStore: Private key permission error detected."
                        + " This likely means the running user does not have RACF authority to access"
                        + " private keys in keyring " + userId + "/" + ringName + "."
                        + " Trying Strategy 3 (direct JCERACFKS load)...");
                } else {
                    // Non-private-key error — still try Strategy 3 but log the original error
                    System.err.println("DEBUG [Stores] loadKeyringKeyStore: RACFInputStream failed with non-private-key error. Trying Strategy 3...");
                }
            } catch (ClassNotFoundException cnfe) {
                System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 2 FAILED (class not found): " + cnfe.getMessage());
            } catch (Exception e) {
                System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 2 FAILED: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        // Strategy 3: Direct JCERACFKS KeyStore load using IBMZSecurity provider
        // On IBM z/OS JDK, the JCERACFKS KeyStore SPI can load keyrings directly
        // when the keyring URL is provided as the password/loadStoreParameter.
        // This approach does NOT use RACFInputStream and may handle inaccessible
        // private keys gracefully (loading only certificates the user can access).
        Provider ibmZSecurityProvider = Security.getProvider("IBMZSecurity");
        if (ibmZSecurityProvider != null) {
            System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3 - Direct JCERACFKS via IBMZSecurity provider...");
            try {
                KeyStore ks = KeyStore.getInstance(type, ibmZSecurityProvider);
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: KeyStore.getInstance('" + type + "', IBMZSecurity) succeeded.");

                // Try loading with the keyring URL as the password
                // Some IBM implementations accept safkeyring://userId/ring as load parameter
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3a - load(null, ringUrl as password)...");
                try {
                    ks.load(null, formatted.toCharArray());
                    System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3a SUCCEEDED. Aliases count=" + ks.size());
                    return ks;
                } catch (Exception e3a) {
                    System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3a FAILED: " + e3a.getClass().getName() + ": " + e3a.getMessage());
                }

                // Try loading with a DomainLoadStoreParameter-style approach
                // Pass the ring name in format "userId/ringName" as password
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3b - load(null, userId/ringName as password)...");
                try {
                    ks = KeyStore.getInstance(type, ibmZSecurityProvider);
                    ks.load(null, (userId + "/" + ringName).toCharArray());
                    System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3b SUCCEEDED. Aliases count=" + ks.size());
                    return ks;
                } catch (Exception e3b) {
                    System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3b FAILED: " + e3b.getClass().getName() + ": " + e3b.getMessage());
                }

                // Try with standard password and null stream
                System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3c - load(null, 'password')...");
                try {
                    ks = KeyStore.getInstance(type, ibmZSecurityProvider);
                    ks.load(null, password);
                    System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3c SUCCEEDED. Aliases count=" + ks.size());
                    if (ks.size() > 0) {
                        return ks;
                    }
                    System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3c returned empty keystore, not useful.");
                } catch (Exception e3c) {
                    System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3c FAILED: " + e3c.getClass().getName() + ": " + e3c.getMessage());
                }
            } catch (Exception e3) {
                System.err.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3 setup FAILED: " + e3.getClass().getName() + ": " + e3.getMessage());
                e3.printStackTrace(System.err);
            }
        } else {
            System.out.println("DEBUG [Stores] loadKeyringKeyStore: Strategy 3 SKIPPED (IBMZSecurity provider not available)");
        }

        // All strategies failed
        String errorMsg = "Cannot load keyring '" + uri + "': all strategies exhausted.\n"
            + "  - URL handler: safkeyring protocol handler not found in this JDK.\n"
            + "  - RACFInputStream: likely failed due to RACF authority.\n"
            + "Ensure the user running 'zwe validate' has READ access to the keyring.\n"
            + "RACF commands to verify/grant access:\n"
            + "  RLIST FACILITY IRR.DIGTCERT.LISTRING ALL\n"
            + "  PERMIT IRR.DIGTCERT.LISTRING CLASS(FACILITY) ID(<running-user>) ACCESS(READ)\n"
            + "  SETROPTS RACLIST(FACILITY) REFRESH";
        System.err.println("DEBUG [Stores] loadKeyringKeyStore: ALL STRATEGIES FAILED.");
        System.err.println(errorMsg);
        throw new IOException(errorMsg);
    }
}
