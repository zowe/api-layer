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

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustedCertificatesProviderTest {

    private static final String VALID_CERTIFICATE =
        "-----BEGIN CERTIFICATE-----\n" +
            "MIID7zCCAtegAwIBAgIED0TPEjANBgkqhkiG9w0BAQsFADB6MQswCQYDVQQGEwJD\n" +
            "WjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUxFDASBgNVBAoTC1pv\n" +
            "d2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMRUwEwYDVQQD\n" +
            "Ewxab3dlIFNlcnZpY2UwHhcNMTgxMjA3MTQ1NzIyWhcNMjgxMjA0MTQ1NzIyWjB6\n" +
            "MQswCQYDVQQGEwJDWjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUx\n" +
            "FDASBgNVBAoTC1pvd2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExh\n" +
            "eWVyMRUwEwYDVQQDEwxab3dlIFNlcnZpY2UwggEiMA0GCSqGSIb3DQEBAQUAA4IB\n" +
            "DwAwggEKAoIBAQC6Orc/EJ5/t2qam1DiYU/xVbHaQrjd6uvpj2HTvOOohtFZ7/Kx\n" +
            "yMAezgB8DBR4+77qXXsdP9ngnTl/i22yGwvo7Tlz6dhnQLnks7VFr1eGGC2ks+rL\n" +
            "BJsF/RQexmONG9ddexWD8SOYoW9RRapQqETbcllxOenvzXruOEzaXhMazkK9Cg+J\n" +
            "ucNb9HcfhIM0rjLZhqG8Gc8dAtCcxF/xHlVyFQq8fr4u2p/wGmARM14iZeQltQV7\n" +
            "F3gxmw3djfcNM5S3tirPrHlZb76ZmmQEn4QiLSP198Lm+4QKAOw1dUpMf4eELO4c\n" +
            "EFUHXQUCHLWc5NztZxWW40NrDbZEjcRI5ah7AgMBAAGjfTB7MB0GA1UdJQQWMBQG\n" +
            "CCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIV\n" +
            "bG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3QwHQYDVR0OBBYEFHL1ygBb\n" +
            "UCI/ktdk3TgQA6EJlATIMA0GCSqGSIb3DQEBCwUAA4IBAQBHALBlFf0P1TBR1MHQ\n" +
            "vXYDFAW+PiyF7zP0HcrvQTAGYhF7uJtRIamapjUdIsDVbqY0RhoFnBOu8ti2z0pW\n" +
            "djw47f3X/yj98n+J2aYcO64Ar+ovx93P01MA8+Mz1u/LwXk4pmrbUIcOEtyNu+vT\n" +
            "a0jDobC++3Zfv5Y+iD2M8L+jacSMZNCqQByhKtTkAICXg9LMccx4XLYtJ65zGP2h\n" +
            "4TEK0MMfO2G1/vUmdb3tq17zKdukj3MUS254mENCck7ioNFR0Cc9lzuSHyBrdb0x\n" +
            "M/iHeamNblckK/r1roDjhCAQz9DtmETad/o7qGNFxDTRRShRV9Lww0fFB7PaV7u/\n" +
            "VPx2\n" +
            "-----END CERTIFICATE-----";

    private static final String VALID_CERT_SUBJECT_DN =
        "CN=Zowe Service,OU=API Mediation Layer,O=Zowe Sample,L=Prague,ST=Prague,C=CZ";

    private static final String CERTS_URL = "https://localhost/gateway/certificates";

    @Mock
    private CloseableHttpClient closeableHttpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private HttpEntity responseEntity;
    private TrustedCertificatesProvider provider;

    @Nested
    class GivenResponseWithValidCertificate {

        @Test
        void whenGetTrustedCerts_thenCertificatesReturned() throws UnsupportedOperationException, IOException {
            when(httpResponse.getCode()).thenReturn(HttpStatus.SC_OK);
            when(httpResponse.getEntity()).thenReturn(responseEntity);
            when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream(VALID_CERTIFICATE.getBytes()));
            when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertEquals(1, result.size());

            X509Certificate trustedCert = (X509Certificate) result.get(0);
            assertEquals(VALID_CERT_SUBJECT_DN, trustedCert.getSubjectX500Principal().getName());
        }

        @Test
        void whenInvalidUrl_thenNoCertificatesReturned() {
            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts("htpp>\\\\//wrong.url");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void whenIOError_thenNoCertificatesReturned() throws IOException {
            when(closeableHttpClient.execute(any())).thenThrow(new IOException("communication error"));
            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GivenResponseWithInvalidCertificate {

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() throws IOException {
            provider = new TrustedCertificatesProvider(closeableHttpClient);
            when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
            when(httpResponse.getEntity()).thenReturn(responseEntity);
            when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream("invalid_certificate".getBytes()));
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
            // check for log message
        }
    }

    @Nested
    class GivenEmptyResponse {

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() throws UnsupportedOperationException, IOException {
            when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
            when(httpResponse.getEntity()).thenReturn(responseEntity);
            when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream(new byte[0]));

            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void whenNoHttpEntity_thenNoCertificatesReturned() throws IOException {
            when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
            when(httpResponse.getEntity()).thenReturn(responseEntity);
            when(httpResponse.getEntity()).thenReturn(null);

            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GivenErrorResponseCode {

        @BeforeEach
        void setup() throws IOException {
            when(httpResponse.getCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
            when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
            when(httpResponse.getEntity()).thenReturn(responseEntity);
        }

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() throws IOException {
            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
            //check for log message
        }

        @Test
        void whenNoStatusLine_thenNoCertificatesReturned() {
            provider = new TrustedCertificatesProvider(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
            //check for log message
        }
    }
}
