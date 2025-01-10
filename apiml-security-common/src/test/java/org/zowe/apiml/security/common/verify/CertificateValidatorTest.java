/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.utils.X509Utils;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CertificateValidatorTest {

    private static final String URL_PROVIDE_TWO_TRUSTED_CERTS = "trusted_two_certs_url";
    private static final String URL_PROVIDE_THIRD_TRUSTED_CERT = "third_trusted_cert_url";
    private static final String URL_WITH_NO_TRUSTED_CERTS = "invalid_url_for_trusted_certs";
    private static final X509Certificate cert1 = X509Utils.getCertificate(X509Utils.correctBase64("correct_certificate_1"));
    private static final X509Certificate cert2 = X509Utils.getCertificate(X509Utils.correctBase64("correct_certificate_2"));
    private static final X509Certificate cert3 = X509Utils.getCertificate(X509Utils.correctBase64("correct_certificate_3"));
    private CertificateValidator certificateValidator;
    private Set<String> publicKeys;

    @BeforeEach
    void setUp() {
        TrustedCertificatesProvider mockProvider = mock(TrustedCertificatesProvider.class);
        when(mockProvider.getTrustedCerts(URL_PROVIDE_TWO_TRUSTED_CERTS)).thenReturn(Arrays.asList(cert1, cert2));
        when(mockProvider.getTrustedCerts(URL_PROVIDE_THIRD_TRUSTED_CERT)).thenReturn(Collections.singletonList(cert3));
        when(mockProvider.getTrustedCerts(URL_WITH_NO_TRUSTED_CERTS)).thenReturn(Collections.emptyList());
        certificateValidator = new CertificateValidator(mockProvider, Collections.emptySet());
    }

    @Nested
    class WhenTrustedCertsProvided {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(certificateValidator, "proxyCertificatesEndpoints", new String[] {URL_PROVIDE_TWO_TRUSTED_CERTS});
        }
        @Test
        void whenAllCertificatesFoundThenTheyAreTrusted() {
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert1}));
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert2}));
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert1, cert2}));
        }

        @Test
        void whenSomeCertificateNotFoundThenAllUntrusted() {
            assertFalse(certificateValidator.isTrusted(new X509Certificate[]{cert3}));
            assertFalse(certificateValidator.isTrusted(new X509Certificate[]{cert1, cert3}));
            assertFalse(certificateValidator.isTrusted(new X509Certificate[]{cert2, cert3}));
        }
    }

    @Nested
    class WhenNoTrustedCertsProvided {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(certificateValidator, "proxyCertificatesEndpoints", new String[] {URL_WITH_NO_TRUSTED_CERTS});
        }
        @Test
        void thenAnyCertificateIsNotTrusted() {
            assertFalse(certificateValidator.isTrusted(new X509Certificate[]{cert1}));
            assertFalse(certificateValidator.isTrusted(new X509Certificate[]{cert2}));
            assertFalse(certificateValidator.isTrusted(new X509Certificate[]{cert3}));
        }
    }

    @Nested
    class WhenUpdatePublicKeys {

        @BeforeEach
        void setUp() {
            publicKeys = Stream.of("public_key_1", "public_key_2").collect(Collectors.toCollection(HashSet::new));
            certificateValidator = new CertificateValidator(mock(TrustedCertificatesProvider.class), publicKeys);
        }

        @Test
        void whenUpdatePublicKeys_thenSetUpdated() {
            certificateValidator.updateAPIMLPublicKeyCertificates(new X509Certificate[]{cert1, cert2, cert3});

            assertEquals(5, publicKeys.size());
            assertTrue(publicKeys.contains(Base64.getEncoder().encodeToString(cert1.getPublicKey().getEncoded())));
            assertTrue(publicKeys.contains(Base64.getEncoder().encodeToString(cert2.getPublicKey().getEncoded())));
            assertTrue(publicKeys.contains(Base64.getEncoder().encodeToString(cert3.getPublicKey().getEncoded())));
        }

    }

    @Nested
    class WhenMultipleSources {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(certificateValidator, "proxyCertificatesEndpoints", new String[] {URL_PROVIDE_TWO_TRUSTED_CERTS, URL_PROVIDE_THIRD_TRUSTED_CERT});
        }

        @Test
        void whenAllCertificatesFoundThenTheyAreTrusted() {
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert1}));
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert2}));
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert3}));
            assertTrue(certificateValidator.isTrusted(new X509Certificate[]{cert1, cert3}));
        }

    }

    @Nested
    @Import(CertificateValidator.class)
    @MockBean(TrustedCertificatesProvider.class)
    class Configuration {

        @Nested
        @TestPropertySource(properties = {
            "apiml.security.x509.certificatesUrl=url1"
        })
        @ExtendWith(SpringExtension.class)
        class OldPropertyValue {

            @Autowired
            private CertificateValidator certificateValidator;

            @Test
            void thenUrlIsSetAsListCorrectly() {
                assertArrayEquals(new String[] {"url1"}, (String[]) ReflectionTestUtils.getField(certificateValidator, "proxyCertificatesEndpoints"));
            }

        }

        @Nested
        @TestPropertySource(properties = {
            "apiml.security.x509.certificatesUrls=url1,url2"
        })
        @ExtendWith(SpringExtension.class)
        class NewPropertyValue {

            @Autowired
            private CertificateValidator certificateValidator;

            @Test
            void thenUrlsAreSetCorrectly() {
                assertArrayEquals(new String[] {"url1", "url2"}, (String[]) ReflectionTestUtils.getField(certificateValidator, "proxyCertificatesEndpoints"));
            }

        }

        @Nested
        @ExtendWith(SpringExtension.class)
        class NoUrlIsProvided {

            @Autowired
            private CertificateValidator certificateValidator;

            @Test
            void thenNoUrlIsProvided() {
                assertArrayEquals(new String[0], (String[]) ReflectionTestUtils.getField(certificateValidator, "proxyCertificatesEndpoints"));
            }

        }

    }

}
