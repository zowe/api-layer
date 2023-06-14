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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    private static final String KEY_ALIAS = "localhost";
    private static final String JWT_KEY_ALIAS = "localhost";
    private static final String WRONG_PARAMETER = "wrong";
    private static final String PUBLIC_KEY_FILE = "jwt-public-key.pub";

    private HttpsConfig.HttpsConfigBuilder httpsConfigBuilder;

    @BeforeEach
    void setUp() {
        httpsConfigBuilder = SecurityTestUtils.correctHttpsSettings();
    }

    @Nested
    class whenLoadingKey {
        @Test
        void givenValidSetup_thenReturnValidKey() {
            HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(JWT_KEY_ALIAS).build();
            Key secretKey = SecurityUtils.loadKey(httpsConfig);
            assertNotNull(secretKey);
        }

        @Test
        void givenIncorrectPassword_thenThrowException() {
            HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(JWT_KEY_ALIAS).keyPassword(WRONG_PARAMETER.toCharArray()).build();
            assertThrows(HttpsConfigError.class, () -> SecurityUtils.loadKey(httpsConfig));
        }

        @Test
        void givenNullAsKeyAlias_thenThrowException() {
            HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(null).build();
            assertThrows(HttpsConfigError.class, () -> SecurityUtils.loadKey(httpsConfig));
        }
    }

    @Test
    void testLoadPublicKey() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(JWT_KEY_ALIAS).build();
        PublicKey publicKey = SecurityUtils.loadPublicKey(httpsConfig);
        assertNotNull(publicKey);
    }

    @Test
    void testLoadPublicKeyWithBadKeyStore() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyStore("/localhost.truststore.p12").build();
        assertThrows(HttpsConfigError.class, () -> SecurityUtils.loadPublicKey(httpsConfig));
    }

    @Test
    void testFindPrivateKeyByPublic() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        byte[] publicKey = loadPublicKeyFromFile();
        Key secretKey = SecurityUtils.findPrivateKeyByPublic(httpsConfig, publicKey);
        assertNotNull(secretKey);
    }

    @Test
    void testFindPrivateKeyByPublicWithIncorrectKeyPassword() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyPassword(WRONG_PARAMETER.toCharArray()).build();
        byte[] publicKey = loadPublicKeyFromFile();
        assertThrows(HttpsConfigError.class, () -> SecurityUtils.findPrivateKeyByPublic(httpsConfig, publicKey));
    }

    @Test
    void testLoadKeyStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        KeyStore keyStore = SecurityUtils.loadKeyStore(httpsConfig);
        assertTrue(keyStore.size() > 0);
    }

    @Test
    void testReplaceFourSlashes() {
        String newUrl = SecurityUtils.replaceFourSlashes("safkeyring:////userId/keyRing");
        assertEquals("safkeyring://userId/keyRing", newUrl);
    }

    @Test
    void testGenerateKeyPair() {
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        assertNotNull(keyPair);
    }

    private byte[] loadPublicKeyFromFile() {
        byte[] publicKey = null;
        try {
            String keyInBase64 = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(PUBLIC_KEY_FILE).toURI())));
            keyInBase64 = keyInBase64.replace("\n", "").replace("\r", "");
            publicKey = Base64.getDecoder().decode(keyInBase64);
        } catch (IOException | URISyntaxException e) {
            fail("Error reading secret key from file " + PUBLIC_KEY_FILE + ": " + e.getMessage());
        }
        return publicKey;
    }

    @Test
    void loadCertificateChainNoKeystore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = HttpsConfig.builder().build();
        Certificate[] certificates = SecurityUtils.loadCertificateChain(httpsConfig);
        assertEquals(0, certificates.length);
    }

    @Test
    void loadCertificateChain() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(KEY_ALIAS).build();
        Certificate[] certificates = SecurityUtils.loadCertificateChain(httpsConfig);
        assertTrue(certificates.length > 0);
    }

    @Test
    void loadCertificateChainBase64() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(KEY_ALIAS).build();
        Set<String> certificatesBase64 = SecurityUtils.loadCertificateChainBase64(httpsConfig);
        assertFalse(certificatesBase64.isEmpty());
    }

}
