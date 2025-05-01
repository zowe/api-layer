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

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class LocalVerifierTest {

    @Nested
    class MatchingHosts {

        @Test
        void givenMatchingWildCard_whenVerifyHosts_thenReturnTrue() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertTrue(localVerifier.isMatching("subdomain.domain.com", "*.domain.com", Collections.emptyList()));
            assertTrue(localVerifier.isMatching("2.DOMAIN.com", "*.domain.COM", Collections.emptyList()));
        }

        @Test
        void givenNonMatchingWildCard_whenVerifyHosts_thenReturnFalse() {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            assertFalse(localVerifier.isMatching("a.b.domain.com", "*.domain.com", Collections.emptyList()));
            assertFalse(localVerifier.isMatching("b.domain2.com", "*.domain.com", Collections.emptyList()));
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

    }

    @Nested
    class ExtendedKeyUsage {

        @Test
        void givenServerCertUsage_whenVerifyServer_thenReturnTrue() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            X509Certificate cert = mock(X509Certificate.class);
            doReturn(Arrays.asList("1.3.6.1.5.5.7.3.1")).when(cert).getExtendedKeyUsage();
            assertTrue(localVerifier.verifyServer(cert));
        }

        @Test
        void givenNoServerCertUsage_whenVerifyServer_thenReturnFalse() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            X509Certificate cert = mock(X509Certificate.class);
            doReturn(Arrays.asList("a", "b", "c")).when(cert).getExtendedKeyUsage();
            assertFalse(localVerifier.verifyServer(cert));
        }

        @Test
        void givenX509_whenVerifyX509_thenReturnTrue() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            X509Certificate cert = mock(X509Certificate.class);
            doReturn(Arrays.asList("1.3.6.1.5.5.7.3.2")).when(cert).getExtendedKeyUsage();
            assertTrue(localVerifier.verifyX509(cert));
        }

        @Test
        void givenNoX509_whenVerifyX509_thenReturnFalse() throws CertificateParsingException {
            LocalVerifier localVerifier = new LocalVerifier(null, null);
            X509Certificate cert = mock(X509Certificate.class);
            doReturn(Arrays.asList("1.3.6.1.5.5.7.3", "2")).when(cert).getExtendedKeyUsage();
            assertFalse(localVerifier.verifyX509(cert));
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
