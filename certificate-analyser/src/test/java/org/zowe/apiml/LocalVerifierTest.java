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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class LocalVerifierTest {

    @Nested
    class JwtKey {

        private record Result(boolean valid, String output) {
        }

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

        private PublicKey pss(boolean restricted) throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSASSA-PSS");
            if (restricted) {
                generator.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4,
                    new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1)));
            } else {
                generator.initialize(2048);
            }
            return generator.generateKeyPair().getPublic();
        }

        private Result verify(PublicKey key, String caSignatureAlgorithmOid) {
            X509Certificate certificate = mock(X509Certificate.class);
            doReturn(key).when(certificate).getPublicKey();
            lenient().doReturn(caSignatureAlgorithmOid).when(certificate).getSigAlgOID();

            PrintStream original = System.out;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out, true));
            try {
                boolean valid = new LocalVerifier(null, null).verifyJwt(certificate);
                return new Result(valid, out.toString());
            } finally {
                System.setOut(original);
            }
        }

        private Result verify(PublicKey key) {
            return verify(key, "1.2.840.113549.1.1.11");
        }

        @Test
        void givenRsaKey_thenCanBeUsed() throws Exception {
            Result result = verify(rsa(2048));

            assertTrue(result.valid());
            assertTrue(result.output().contains("Certificate can be used for generating JWT token."));
        }

        @ParameterizedTest
        @CsvSource({
            "1.2.840.113549.1.1.11", // sha256WithRSA
            "1.2.840.113549.1.1.12", // sha384WithRSA
            "1.2.840.113549.1.1.13", // sha512WithRSA
            "1.2.840.10045.4.3.3",   // sha384WithECDSA
            "1.2.840.113549.1.1.10"  // RSASSA-PSS
        })
        void givenRsaKey_whenCertificateSignedByCaWithAnyAlgorithm_thenCanBeUsed(String caSignatureAlgorithmOid) throws Exception {
            assertTrue(verify(rsa(2048), caSignatureAlgorithmOid).valid());
        }

        /** The size of the RSA key is not checked. */
        @ParameterizedTest
        @CsvSource({"1024", "4096"})
        void givenRsaKeyOfAnySize_thenCanBeUsed(int bits) throws Exception {
            assertTrue(verify(rsa(bits)).valid());
        }

        /** An RSASSA-PSS key without parameter restrictions can be used with RS256. */
        @Test
        void givenRsaPssKeyWithoutRestrictions_thenCanBeUsed() throws Exception {
            assertTrue(verify(pss(false)).valid());
        }

        /** An RSASSA-PSS key restricted to PSS parameters cannot be used with RS256. */
        @Test
        void givenRsaPssKeyRestrictedToPssParameters_thenCannotBeUsed() throws Exception {
            Result result = verify(pss(true));

            assertFalse(result.valid());
            assertTrue(result.output().contains("restricted to RSASSA-PSS parameters"));
        }

        /** Only RSA keys are supported. */
        @ParameterizedTest
        @CsvSource({"secp256r1", "secp384r1"})
        void givenEcKey_thenCannotBeUsed(String curve) throws Exception {
            Result result = verify(ec(curve));

            assertFalse(result.valid());
            assertTrue(result.output().contains("of type EC, only RSA keys are supported"));
        }

        @Test
        void givenNoKey_thenCannotBeUsed() {
            Result result = verify(null);

            assertFalse(result.valid());
            assertTrue(result.output().contains("The key is missing"));
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
