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

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Set;

import static org.junit.Assert.*;

public class SecurityUtilsTest {

    private static final String KEY_ALIAS = "localhost";
    private static final String JWT_KEY_ALIAS = "jwtsecret";
    private static final String WRONG_PARAMETER = "wrong";
    private static final String PUBLIC_KEY_FILE = "jwt-public-key.pub";

    private HttpsConfig.HttpsConfigBuilder httpsConfigBuilder;

    @Before
    public void setUp() {
        httpsConfigBuilder = SecurityTestUtils.correctHttpsSettings();
    }

    @Test
    public void testReadSecret() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(KEY_ALIAS).build();
        String secretKey = SecurityUtils.readSecret(httpsConfig);
        assertNotNull(secretKey);
    }

    @Test(expected = HttpsConfigError.class)
    public void testReadSecretWithIncorrectKeyAlias() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(WRONG_PARAMETER).build();
        String secretKey = SecurityUtils.readSecret(httpsConfig);
        assertNull(secretKey);
    }

    @Test
    public void testLoadKey() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(JWT_KEY_ALIAS).build();
        Key secretKey = SecurityUtils.loadKey(httpsConfig);
        assertNotNull(secretKey);
    }

    @Test(expected = HttpsConfigError.class)
    public void testLoadKeyWithIncorrectKeyPassword() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(JWT_KEY_ALIAS).keyPassword(WRONG_PARAMETER).build();
        Key secretKey = SecurityUtils.loadKey(httpsConfig);
        assertNull(secretKey);
    }

    @Test
    public void testLoadPublicKey() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(JWT_KEY_ALIAS).build();
        PublicKey publicKey = SecurityUtils.loadPublicKey(httpsConfig);
        assertNotNull(publicKey);
    }

    @Test(expected = HttpsConfigError.class)
    public void testLoadPublicKeyWithBadKeyStore() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyStore("/localhost.truststore.p12").build();
        PublicKey publicKey = SecurityUtils.loadPublicKey(httpsConfig);
        assertNull(publicKey);
    }

    @Test
    public void testFindPrivateKeyByPublic() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        byte[] publicKey = loadPublicKeyFromFile();
        Key secretKey = SecurityUtils.findPrivateKeyByPublic(httpsConfig, publicKey);
        assertNotNull(secretKey);
    }

    @Test(expected = HttpsConfigError.class)
    public void testFindPrivateKeyByPublicWithIncorrectKeyPassword() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyPassword(WRONG_PARAMETER).build();
        byte[] publicKey = loadPublicKeyFromFile();
        Key secretKey = SecurityUtils.findPrivateKeyByPublic(httpsConfig, publicKey);
        assertNull(secretKey);
    }

    @Test
    public void testLoadKeyStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        KeyStore keyStore = SecurityUtils.loadKeyStore(httpsConfig);
        assertTrue(keyStore.size() > 0);
    }

    @Test
    public void testReplaceFourSlashes() {
        String newUrl = SecurityUtils.replaceFourSlashes("safkeyring:////userId/keyRing");
        assertEquals("safkeyring://userId/keyRing", newUrl);
    }

    @Test
    public void testGenerateKeyPair() {
        KeyPair keyPair = SecurityUtils.generateKeyPair("RSA", 2048);
        assertNotNull(keyPair);
    }

    private byte[] loadPublicKeyFromFile() {
        byte[] publicKey = null;
        try {
            String keyInBase64 = new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource(PUBLIC_KEY_FILE).toURI())));
            publicKey = Base64.getDecoder().decode(keyInBase64);
        } catch (IOException | URISyntaxException e) {
            fail("Error reading secret key from file " + PUBLIC_KEY_FILE + ": " + e.getMessage());
        }
        return publicKey;
    }

    @Test
    public void loadCertificateChainNoKeystore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = HttpsConfig.builder().build();
        Certificate[] certificates = SecurityUtils.loadCertificateChain(httpsConfig);
        assertEquals(certificates.length, 0);
    }

    @Test
    public void loadCertificateChain() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(KEY_ALIAS).build();
        Certificate[] certificates = SecurityUtils.loadCertificateChain(httpsConfig);
        assertTrue(certificates.length > 0);
    }

    @Test
    public void loadCertificateChainBase64() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyAlias(KEY_ALIAS).build();
        Set<String> certificatesBase64 = SecurityUtils.loadCertificateChainBase64(httpsConfig);
        assertFalse(certificatesBase64.isEmpty());
    }

}
