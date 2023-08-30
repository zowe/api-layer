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

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private static final String VALID_CERT_PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAujq3PxCef7dqmptQ4mFP8VWx2k" +
            "K43err6Y9h07zjqIbRWe/yscjAHs4AfAwUePu+6l17HT/Z4J05f4ttshsL6O05c+nY" +
            "Z0C55LO1Ra9XhhgtpLPqywSbBf0UHsZjjRvXXXsVg/EjmKFvUUWqUKhE23JZcTnp78" +
            "167jhM2l4TGs5CvQoPibnDW/R3H4SDNK4y2YahvBnPHQLQnMRf8R5VchUKvH6+Ltqf" +
            "8BpgETNeImXkJbUFexd4MZsN3Y33DTOUt7Yqz6x5WW++mZpkBJ+EIi0j9ffC5vuECg" +
            "DsNXVKTH+HhCzuHBBVB10FAhy1nOTc7WcVluNDaw22RI3ESOWoewIDAQAB";

    private static final String VALID_CERT_SUBJECT_DN =
        "CN=Zowe Service,OU=API Mediation Layer,O=Zowe Sample,L=Prague,ST=Prague,C=CZ";

    private TrustedCertificatesProvider provider;
    private Set<String> publicKeys;
    private CloseableHttpClient closeableHttpClient;
    private CloseableHttpResponse httpResponse;
    private StatusLine statusLine;
    private HttpEntity responseEntity;

    @BeforeEach
    void setup() throws IOException {
        closeableHttpClient = mock(CloseableHttpClient.class);
        httpResponse = mock(CloseableHttpResponse.class);
        statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
        responseEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(responseEntity);
    }

    @Nested
    class GivenResponseWithValidCertificate {

        @BeforeEach
        void setup() throws IOException {
            when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream(VALID_CERTIFICATE.getBytes()));
        }

        @Nested
        class WhenPublicKeysExists {

            @BeforeEach
            void setup() {
                publicKeys = Stream.of("public_key_1", "public_key_2").collect(Collectors.toCollection(HashSet::new));
            }

            @Test
            void whenGetTrustedCerts_thenCertificatesReturned() {
                provider = new TrustedCertificatesProvider(closeableHttpClient, publicKeys);
                List<Certificate> result = provider.getTrustedCerts("certificates/endpoint");
                assertNotNull(result);
                assertEquals(1, result.size());

                X509Certificate trustedCert = (X509Certificate) result.get(0);
                assertEquals(VALID_CERT_SUBJECT_DN, trustedCert.getSubjectX500Principal().getName());
                assertEquals(3, publicKeys.size());
                assertTrue(publicKeys.contains(VALID_CERT_PUBLIC_KEY));
            }
        }

        @Nested
        class WhenNoPublicKeys {

            @BeforeEach
            void setup() {
                publicKeys = new HashSet<>();
            }

            @Test
            void whenGetTrustedCerts_thenCertificatesReturned() {
                provider = new TrustedCertificatesProvider(closeableHttpClient, publicKeys);
                List<Certificate> result = provider.getTrustedCerts("certificates/endpoint");
                assertNotNull(result);
                assertEquals(1, result.size());

                X509Certificate trustedCert = (X509Certificate) result.get(0);
                assertEquals(VALID_CERT_SUBJECT_DN, trustedCert.getSubjectX500Principal().getName());
                assertEquals(1, publicKeys.size());
                assertTrue(publicKeys.contains(VALID_CERT_PUBLIC_KEY));
            }
        }
    }

    @Nested
    class GivenResponseWithInvalidCertificate {
        @BeforeEach
        void setup() throws IOException {
            when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream("invalid_certificate".getBytes()));
            publicKeys = Stream.of("public_key_1", "public_key_2").collect(Collectors.toCollection(HashSet::new));
        }

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() {
            provider = new TrustedCertificatesProvider(closeableHttpClient, publicKeys);
            List<Certificate> result = provider.getTrustedCerts("certificates/endpoint");
            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(2, publicKeys.size());
            // check for log message
        }
    }

    @Nested
    class GivenEmptyResponse {
        @BeforeEach
        void setup() throws IOException {
            when(responseEntity.getContent()).thenReturn(new ByteArrayInputStream(new byte[0]));
            publicKeys = Stream.of("public_key_1", "public_key_2").collect(Collectors.toCollection(HashSet::new));
        }

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() {
            provider = new TrustedCertificatesProvider(closeableHttpClient, publicKeys);
            List<Certificate> result = provider.getTrustedCerts("certificates/endpoint");
            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(2, publicKeys.size());
        }
    }

    @Nested
    class GivenErrorResponseCode {
        @BeforeEach
        void setup() {
            when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
            publicKeys = Stream.of("public_key_1", "public_key_2").collect(Collectors.toCollection(HashSet::new));
        }

        @Test
        void whenGetTrustedCerts_thenNoCertificatesReturned() {
            provider = new TrustedCertificatesProvider(closeableHttpClient, publicKeys);
            List<Certificate> result = provider.getTrustedCerts("certificates/endpoint");
            assertNotNull(result);
            assertTrue(result.isEmpty());
            assertEquals(2, publicKeys.size());
            //check for log message
        }
    }
}
