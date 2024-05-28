/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class SecurityUtils {

    private ApimlLogger apimlLog = ApimlLogger.of(SecurityUtils.class, YamlMessageServiceInstance.getInstance());

    private static final Pattern KEYRING_PATTERN = Pattern.compile("^(safkeyring[^:]*):/{2,4}([^/]+)/([^/]+)$");

    public boolean isKeyring(String input) {
        if (input == null) return false;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        return matcher.matches();
    }

    public String formatKeyringUrl(String input) {
        if (input == null) return null;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1) + "://" + matcher.group(2) + "/" + matcher.group(3);
        }
        return input;
    }

    public final static String COOKIE_AUTH_NAME = "apimlAuthenticationToken";

    /**
     * Loads secret key from keystore or key ring, if keystore URL has proper format {@link #KEYRING_PATTERN}
     *
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *               and optional filled: keyAlias and trustStore
     * @return {@link PrivateKey} or {@link javax.crypto.SecretKey} from keystore or key ring
     */
    public static PrivateKey loadKey(HttpsConfig config) {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            try {
                KeyStore ks = loadKeyStore(config);
                char[] keyPasswordInChars = config.getKeyPassword();
                final PrivateKey key;
                if (config.getKeyAlias() != null) {
                    key = (PrivateKey) ks.getKey(config.getKeyAlias(), keyPasswordInChars);
                } else {
                    throw new KeyStoreException("No key alias provided.");
                }
                return key;
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                     | UnrecoverableKeyException e) {
                apimlLog.log("org.zowe.apiml.common.errorLoadingSecretKey", e.getMessage());
                throw new HttpsConfigError(e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Return certificate chain of certificate using to sing of HTTPS communication (certificate stored in keystore,
     * under keyAlias, both values are determinated via config)
     *
     * @param config defines path to KeyStore and key alias
     * @return chain of certificates loaded from Keystore to alias keyAlias from HttpsConfig
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static Certificate[] loadCertificateChain(HttpsConfig config) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            KeyStore ks = loadKeyStore(config);
            return ks.getCertificateChain(config.getKeyAlias());
        }
        return new Certificate[0];
    }

    /**
     * Method load certification chain from KeyStore for KeyAlias stored in the HttpsConfig. Then it takes public keys
     * and convert them into Base64 code and return them as a Set.
     *
     * @param config defines path to KeyStore and key alias
     * @return Base64 codes of all public certificates determinated for KeyAlias in KeyStore
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static Set<String> loadCertificateChainBase64(HttpsConfig config) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final Set<String> out = ConcurrentHashMap.newKeySet();
        for (Certificate certificate : loadCertificateChain(config)) {
            final byte[] certificateEncoded = certificate.getPublicKey().getEncoded();
            final String base64 = Base64.getEncoder().encodeToString(certificateEncoded);
            out.add(base64);
        }
        return out;
    }

    /**
     * Loads public key from keystore or key ring, if keystore URL has proper format {@link #KEYRING_PATTERN}
     *
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *               and optional filled: keyAlias and trustStore
     * @return {@link PublicKey} from keystore or key ring
     */
    public static PublicKey loadPublicKey(HttpsConfig config) {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            try {
                KeyStore ks = loadKeyStore(config);
                Certificate cert = null;
                if (config.getKeyAlias() != null) {
                    cert = ks.getCertificate(config.getKeyAlias());
                } else {
                    for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                        String alias = e.nextElement();
                        cert = ks.getCertificate(alias);
                        if (cert != null) {
                            break;
                        }
                    }
                }
                if (cert != null) {
                    return cert.getPublicKey();
                }
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
                apimlLog.log("org.zowe.apiml.common.errorLoadingPublicKey", e.getMessage());
                throw new HttpsConfigError(e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Finds a private key by public key in keystore or key ring, if keystore URL has proper format {@link #KEYRING_PATTERN}
     *
     * @param config    {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                  and optional filled: keyAlias and trustStore
     * @param publicKey in byte[]
     * @return {@link PrivateKey} from keystore or key ring
     */
    public static Key findPrivateKeyByPublic(HttpsConfig config, byte[] publicKey) {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            try {
                KeyStore ks = loadKeyStore(config);
                char[] keyPasswordInChars = config.getKeyPassword();
                Key key = null;
                for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    Certificate cert = ks.getCertificate(alias);
                    if (Arrays.equals(cert.getPublicKey().getEncoded(), publicKey)) {
                        key = ks.getKey(alias, keyPasswordInChars);
                        if (key != null) {
                            break;
                        }
                    }
                }
                return key;
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException |
                     UnrecoverableKeyException e) {
                apimlLog.log("org.zowe.apiml.common.errorLoadingSecretKey", e.getMessage());
                throw new HttpsConfigError("Error loading secret key: " + e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Loads keystore or key ring, if keystore URL has proper format {@link #KEYRING_PATTERN}, from specified location
     *
     * @param type     - type of store
     * @param path     - path or URL of store
     * @param password - password to the store
     * @return the new {@link KeyStore} or key ring as {@link KeyStore}
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public static KeyStore loadKeyStore(String type, String path, char[] password) throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance(type);
        InputStream inputStream;
        if (SecurityUtils.isKeyring(path)) {
            inputStream = new URL(path).openStream();
        } else {
            inputStream = new FileInputStream(path);
        }
        ks.load(inputStream, password);
        return ks;
    }

    /**
     * Loads keystore or key ring, if keystore URL has proper format {@link #KEYRING_PATTERN}, from specified location
     *
     * @param config {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword,
     *               and optional filled: trustStore
     * @return the new {@link KeyStore} or key ring as {@link KeyStore}
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore(HttpsConfig config) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        return loadKeyStore(config.getKeyStoreType(), config.getKeyStore(), config.getKeyStorePassword());
    }

    /**
     * Creates an {@link URL} to key ring location
     *
     * @param uri - key ring location
     * @return the new {@link URL} with 2 slashes instead of 4
     * @throws MalformedURLException throws in case of incorrect key ring format
     */
    public static URL keyRingUrl(String uri) throws MalformedURLException {
        if (!isKeyring(uri)) {
            throw new MalformedURLException("Incorrect key ring format: " + uri
                + ". Make sure you use format safkeyring://userId/keyRing");
        }
        return new URL(formatKeyringUrl(uri));
    }

    /**
     * Creates a pair of {@link PrivateKey} and {@link PublicKey} keys
     *
     * @param algorithm - key algorithm
     * @param keySize   - key size
     * @return the new {@link KeyPair}
     */
    public static KeyPair generateKeyPair(String algorithm, int keySize) {
        KeyPair kp = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            kpg.initialize(keySize);
            kp = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.debug("An error occurred while generating keypair: {}", e.getMessage());
        }
        return kp;
    }

    public static char[] readPassword(Object value) {
        if (value == null) return new char[0];
        if (value instanceof char[]) {
            return (char[]) value;
        }
        if (!(value instanceof String)) {
            value = value.toString();
        }

        return ((String) value).toCharArray();
    }

}
