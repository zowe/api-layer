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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.util.HttpClientMockHelper;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
class TrustedCertificatesProviderTest {

    private static final String VALID_CERTIFICATE =
        """
            -----BEGIN CERTIFICATE-----
            MIID7zCCAtegAwIBAgIED0TPEjANBgkqhkiG9w0BAQsFADB6MQswCQYDVQQGEwJD
            WjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUxFDASBgNVBAoTC1pv
            d2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMRUwEwYDVQQD
            Ewxab3dlIFNlcnZpY2UwHhcNMTgxMjA3MTQ1NzIyWhcNMjgxMjA0MTQ1NzIyWjB6
            MQswCQYDVQQGEwJDWjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUx
            FDASBgNVBAoTC1pvd2UgU2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExh
            eWVyMRUwEwYDVQQDEwxab3dlIFNlcnZpY2UwggEiMA0GCSqGSIb3DQEBAQUAA4IB
            DwAwggEKAoIBAQC6Orc/EJ5/t2qam1DiYU/xVbHaQrjd6uvpj2HTvOOohtFZ7/Kx
            yMAezgB8DBR4+77qXXsdP9ngnTl/i22yGwvo7Tlz6dhnQLnks7VFr1eGGC2ks+rL
            BJsF/RQexmONG9ddexWD8SOYoW9RRapQqETbcllxOenvzXruOEzaXhMazkK9Cg+J
            ucNb9HcfhIM0rjLZhqG8Gc8dAtCcxF/xHlVyFQq8fr4u2p/wGmARM14iZeQltQV7
            F3gxmw3djfcNM5S3tirPrHlZb76ZmmQEn4QiLSP198Lm+4QKAOw1dUpMf4eELO4c
            EFUHXQUCHLWc5NztZxWW40NrDbZEjcRI5ah7AgMBAAGjfTB7MB0GA1UdJQQWMBQG
            CCsGAQUFBwMCBggrBgEFBQcDATAOBgNVHQ8BAf8EBAMCBPAwKwYDVR0RBCQwIoIV
            bG9jYWxob3N0LmxvY2FsZG9tYWlugglsb2NhbGhvc3QwHQYDVR0OBBYEFHL1ygBb
            UCI/ktdk3TgQA6EJlATIMA0GCSqGSIb3DQEBCwUAA4IBAQBHALBlFf0P1TBR1MHQ
            vXYDFAW+PiyF7zP0HcrvQTAGYhF7uJtRIamapjUdIsDVbqY0RhoFnBOu8ti2z0pW
            djw47f3X/yj98n+J2aYcO64Ar+ovx93P01MA8+Mz1u/LwXk4pmrbUIcOEtyNu+vT
            a0jDobC++3Zfv5Y+iD2M8L+jacSMZNCqQByhKtTkAICXg9LMccx4XLYtJ65zGP2h
            4TEK0MMfO2G1/vUmdb3tq17zKdukj3MUS254mENCck7ioNFR0Cc9lzuSHyBrdb0x
            M/iHeamNblckK/r1roDjhCAQz9DtmETad/o7qGNFxDTRRShRV9Lww0fFB7PaV7u/
            VPx2
            -----END CERTIFICATE-----
            """.stripIndent();

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

    @BeforeEach
    void setup() {
        HttpClientMockHelper.mockExecuteWithResponse(closeableHttpClient, httpResponse);
        provider = new TrustedCertificatesProvider(closeableHttpClient);
    }

    @AfterEach
    void tearDown() {
        reset(httpResponse);
    }

    @Nested
    class GivenResponseWithValidCertificate {

        @Test
        void whenGetTrustedCerts_thenCertificatesReturned() throws UnsupportedOperationException {
            HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, VALID_CERTIFICATE);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertEquals(1, result.size());

            X509Certificate trustedCert = (X509Certificate) result.get(0);
            assertEquals(VALID_CERT_SUBJECT_DN, trustedCert.getSubjectX500Principal().getName());
        }

        @Test
        void whenInvalidUrl_thenNoCertificatesReturned() {
            reset(closeableHttpClient);
            List<Certificate> result = provider.getTrustedCerts("htpp>\\\\//wrong.url");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void whenIOError_thenNoCertificatesReturned() {
            reset(closeableHttpClient);
            HttpClientMockHelper.whenExecuteThenThrow(closeableHttpClient, new IOException("communication error"));
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GivenResponseWithInvalidCertificate {

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() {
            HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, "invalid_response_causing_certificate_parsing_error");
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GivenEmptyResponse {

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() {
            HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, "");
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void whenNoHttpEntity_thenNoCertificatesReturned() {
            HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, null);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GivenErrorResponseCode {

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() {
            HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_BAD_REQUEST);
            List<Certificate> result = provider.getTrustedCerts(CERTS_URL);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
