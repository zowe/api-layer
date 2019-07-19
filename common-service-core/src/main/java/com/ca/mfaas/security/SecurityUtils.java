/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
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

@Slf4j
@UtilityClass
public class SecurityUtils {

    public static final String SAFKEYRING = "safkeyring";

    /**
     * Reads secret key from keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}, and encode to Base64
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                 and optional filled: keyAlias and trustStore
     * @return Base64 encoded secret key in {@link String}
     */
    public static String readSecret(HttpsConfig config) {
        if (config.getKeyStore() != null) {
            try {
                Key key = loadKey(config);
                if (key == null) {
                    throw new UnrecoverableKeyException(String.format(
                        "No key with private key entry could be used in the keystore. Provided key alias: %s",
                        config.getKeyAlias() == null ? "<not provided>" : config.getKeyAlias()));
                }
                return Base64.getEncoder().encodeToString(key.getEncoded());
            } catch (UnrecoverableKeyException e) {
                log.error("Error reading secret key: {}", e.getMessage(), e);
                throw new HttpsConfigError("Error reading secret key: " + e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Loads secret key from keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                  and optional filled: keyAlias and trustStore
     * @return {@link PrivateKey} or {@link javax.crypto.SecretKey} from keystore or key ring
     */
    public static Key loadKey(HttpsConfig config) {
        if (config.getKeyStore() != null) {
            try {
                KeyStore ks = loadKeyStore(config);
                char[] keyPasswordInChars = config.getKeyPassword() == null ? null : config.getKeyPassword().toCharArray();
                Key key = null;
                if (config.getKeyAlias() != null) {
                    key = ks.getKey(config.getKeyAlias(), keyPasswordInChars);
                } else {
                    key = findFirstSecretKey(ks, keyPasswordInChars);
                }
                return key;
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                | UnrecoverableKeyException e) {
                String errorMessage = "Error loading secret key: " +  e.getMessage();
                log.error(errorMessage, e);
                throw new HttpsConfigError(errorMessage, e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    private static Key findFirstSecretKey(KeyStore keyStore, char[] keyPasswordInChars) throws KeyStoreException, NoSuchAlgorithmException {
        Key key = null;
        for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            try {
                key = keyStore.getKey(alias, keyPasswordInChars);
                if (key != null) {
                    break;
                }
            } catch (UnrecoverableKeyException uke) {
                log.debug("Key with alias {} could not be used: {}", alias, uke.getMessage());
            }
        }
        return key;
    }

    /**
     * Loads public key from keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                  and optional filled: keyAlias and trustStore
     * @return {@link PublicKey} from keystore or key ring
     */
    public static PublicKey loadPublicKey(HttpsConfig config) {
        if (config.getKeyStore() != null) {
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
                String errorMessage = "Error loading public key: " +  e.getMessage();
                log.error(errorMessage, e);
                throw new HttpsConfigError(errorMessage, e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Finds a private key by public key in keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}
     * @param config {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                 and optional filled: keyAlias and trustStore
     * @param publicKey in byte[]
     * @return {@link PrivateKey} from keystore or key ring
     */
    public static Key findPrivateKeyByPublic(HttpsConfig config, byte[] publicKey) {
        if (config.getKeyStore() != null) {
            try {
                KeyStore ks = loadKeyStore(config);
                char[] keyPasswordInChars = config.getKeyPassword() == null ? null : config.getKeyPassword().toCharArray();
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
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | UnrecoverableKeyException e) {
                log.error("Error loading secret key: {}", e.getMessage(), e);
                throw new HttpsConfigError("Error loading secret key: " + e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Loads keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}, from specified location
     * @param config {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword,
     *                                 and optional filled: trustStore
     * @return the new {@link KeyStore} or key ring as {@link KeyStore}
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore(HttpsConfig config) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(config.getKeyStoreType());
        InputStream inputStream;
        if (config.getKeyStore().startsWith(SAFKEYRING)) {
            URL url = keyRingUrl(config.getKeyStore(), config.getTrustStore());
            inputStream = url.openStream();
        } else {
            File keyStoreFile = new File(config.getKeyStore());
            inputStream = new FileInputStream(keyStoreFile);
        }
        ks.load(inputStream, config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray());
        return ks;
    }

    /**
     * Creates an {@link URL} to key ring location
     * @param uri - key ring location
     * @param trustStore - truststore location
     * @return the new {@link URL} with 2 slashes instead of 4
     * @throws MalformedURLException throws in case of incorrect key ring format
     */
    public static URL keyRingUrl(String uri, String trustStore) throws MalformedURLException {
        if (!uri.startsWith(SAFKEYRING + ":////")) {
            throw new MalformedURLException("Incorrect key ring format: " + trustStore
                + ". Make sure you use format safkeyring:////userId/keyRing");
        }
        return new URL(replaceFourSlashes(uri));
    }

    /**
     * Replaces 4 slashes on 2 in URI
     * @param storeUri - URI as {@link String}
     * @return same URI, but with 2 slashes, or null, if {@param storeUri} null
     */
    public static String replaceFourSlashes(String storeUri) {
        return storeUri == null ? null : storeUri.replaceFirst("////", "//");
    }

    /**
     * Creates a pair of {@link PrivateKey} and {@link PublicKey} keys
     * @param algorithm - key algorithm
     * @param keySize - key size
     * @return the new {@link KeyPair}
     */
    public static KeyPair generateKeyPair(String algorithm, int keySize) {
        KeyPair kp = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            kpg.initialize(keySize);
            kp = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }
        return kp;
    }
}
