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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class LocalVerifierTest {

    @Nested
    class JwtKey {

        private PublicKey rsa(int bits) throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bits);
            return generator.generateKeyPair().getPublic();
        }

        private PublicKey ec(String curve) throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(curve));
            return generator.generateKeyPair().getPublic();
        }

        private X509Certificate certificateWith(PublicKey key, String signatureAlgorithmOid) {
            X509Certificate certificate = mock(X509Certificate.class);
            doReturn(key).when(certificate).getPublicKey();
            lenient().doReturn(signatureAlgorithmOid).when(certificate).getSigAlgOID();
            return certificate;
        }

        @ParameterizedTest
        @CsvSource({
            "1.2.840.113549.1.1.11", // sha256WithRSA
            "1.2.840.113549.1.1.12", // sha384WithRSA
            "1.2.840.113549.1.1.13", // sha512WithRSA
            "1.2.840.10045.4.3.3",   // sha384WithECDSA
            "1.2.840.113549.1.1.10"  // RSASSA-PSS
        })
        void givenRsaKey_whenSignedByCaWithAnyAlgorithm_thenCanBeUsed(String signatureAlgorithmOid) throws Exception {
            assertTrue(new LocalVerifier(null, null).verifyJwt(certificateWith(rsa(2048), signatureAlgorithmOid)));
        }

        @Test
        void givenRsaKeyShorterThan2048_thenCannotBeUsed() throws Exception {
            assertFalse(new LocalVerifier(null, null).verifyJwt(certificateWith(rsa(1024), "1.2.840.113549.1.1.11")));
        }

        @Test
        void givenEcKeyOnP256_thenCanBeUsed() throws Exception {
            assertTrue(new LocalVerifier(null, null).verifyJwt(certificateWith(ec("secp256r1"), "1.2.840.10045.4.3.2")));
        }

        @Test
        void givenEcKeyOnOtherCurve_thenCannotBeUsed() throws Exception {
            assertFalse(new LocalVerifier(null, null).verifyJwt(certificateWith(ec("secp384r1"), "1.2.840.10045.4.3.3")));
        }

        /** secp256k1 has a 256-bit field like P-256, but it cannot be used for ES256. */
        @Test
        void givenEcKeyOn256BitCurveOtherThanP256_thenCannotBeUsed() {
            ECParameterSpec secp256k1 = new ECParameterSpec(
                new EllipticCurve(
                    new ECFieldFp(new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)),
                    BigInteger.ZERO, BigInteger.valueOf(7)),
                new ECPoint(
                    new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16),
                    new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)),
                new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16),
                1);
            ECPublicKey key = mock(ECPublicKey.class);
            doReturn(secp256k1).when(key).getParams();

            assertEquals(256, secp256k1.getCurve().getField().getFieldSize());
            assertFalse(LocalVerifier.isJwtSupportedKey(key));
        }

        @Test
        void givenEcKeyWithoutParameters_thenCannotBeUsed() {
            assertFalse(LocalVerifier.isJwtSupportedKey(mock(ECPublicKey.class)));
        }

        @Test
        void givenOtherKeyType_thenCannotBeUsed() {
            assertFalse(LocalVerifier.isJwtSupportedKey(null));
            assertFalse(LocalVerifier.isJwtSupportedKey(mock(PublicKey.class)));
        }
    }

    @Nested
    class MatchingHosts {

        @Test
        void givenMatchingWildCard_whenVerifyHosts_thenReturnTrue() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertTrue(localVerifier.isMatching("subdomain.domain.com", "*.domain.com", Collections.emptyList()));
            assertTrue(localVerifier.isMatching("2.DOMAIN.com", "*.domain.COM", Collections.emptyList()));
            assertTrue(localVerifier.isMatching("2.domain.com", "*.mydomain.COM", Collections.singletonList("*.domain.com")));
        }

        @Test
        void givenNonMatchingWildCard_whenVerifyHosts_thenReturnFalse() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertFalse(localVerifier.isMatching("a.b.domain.com", "*.domain.com", Collections.emptyList()));
            assertFalse(localVerifier.isMatching("b.domain2.com", "*.domain.com", Collections.emptyList()));
            assertFalse(localVerifier.isMatching("b.domain2.com", "*.domain.net", Collections.singletonList("*.domain.com")));
        }

        @Test
        void givenMatchingAlternativeNames_whenVerifyHosts_thenReturnTrue() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertTrue(localVerifier.isMatching("www.a.net", "*.domain.com", Arrays.asList(
                "www.a.net", "WWW.B.NET"
            )));
            assertTrue(localVerifier.isMatching("www.B.net", "*.domain.com", Arrays.asList(
                "www.a.net", "WWW.B.NET"
            )));
        }

        @Test
        void givenNonMatchingAlternativeNames_whenVerifyHosts_thenReturnFalse() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertFalse(localVerifier.isMatching("www.c.net", "*.domain.com", Arrays.asList(
                "www.a.net", "WWW.B.NET"
            )));
        }

        @Test
        void givenExactAlternativeNames_whenVerifyHosts_thenReturnTrue() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertTrue(localVerifier.isMatching("www.domain.net", "*.domain.com", Arrays.asList(
                "www.domain.net"
            )));
        }

        @Test
        void givenInvalidWildCard_whenVerifyHosts_thenReturnFalse() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertFalse(localVerifier.isMatching("mydomain.com", "*domain.com", Collections.emptyList()));
        }

    }

    @Nested
    class ExtendedKeyUsage {

        @Test
        void givenServerCertUsage_whenVerifyServer_thenReturnTrue() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);

            assertTrue(localVerifier.verifyServer(Arrays.asList("1.3.6.1.5.5.7.3.1")));
        }

        @Test
        void givenNoServerCertUsage_whenVerifyServer_thenReturnFalse() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertFalse(localVerifier.verifyServer(Arrays.asList("a", "b", "c")));
        }

        @Test
        void givenX509_whenVerifyX509_thenReturnTrue() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertTrue(localVerifier.verifyX509(Arrays.asList("1.3.6.1.5.5.7.3.2")));
        }

        @Test
        void givenNoX509_whenVerifyX509_thenReturnFalse() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertFalse(localVerifier.verifyX509(Arrays.asList("1.3.6.1.5.5.7.3", "2")));
        }

    }

    @Nested
    class Expiration {

        @Test
        void givenExpiredCertificate_whenVerifyExpired_thenReturnFalse() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            X509Certificate cert = mock(X509Certificate.class);
            doReturn(new Date(0)).when(cert).getNotAfter();
            assertFalse(localVerifier.verifyExpiration(cert));
        }

        @Test
        void givenNotExpiredCertificate_whenVerifyExpired_thenReturnTrue() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            X509Certificate cert = mock(X509Certificate.class);
            doReturn(new Date(System.currentTimeMillis() + 1_000_000)).when(cert).getNotAfter();
            assertTrue(localVerifier.verifyExpiration(cert));
        }

    }

}
