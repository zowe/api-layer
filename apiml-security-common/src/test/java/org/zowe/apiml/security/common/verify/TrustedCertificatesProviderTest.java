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
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.util.HttpClientMockHelper;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.security.common.verify.TrustedCertificatesProvider.BEGIN_CERT;
import static org.zowe.apiml.security.common.verify.TrustedCertificatesProvider.END_CERT;

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

    private static final String VALID_CERTIFICATE_CHAIN =
        """
            -----BEGIN CERTIFICATE-----
            MIIDeTCCAv+gAwIBAgIQCwDpLU1tcx/KMFnHyx4YhjAKBggqhkjOPQQDAzBhMQsw
            CQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3d3cu
            ZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBHMzAe
            Fw0yMTA0MTQwMDAwMDBaFw0zMTA0MTMyMzU5NTlaMFkxCzAJBgNVBAYTAlVTMRUw
            EwYDVQQKEwxEaWdpQ2VydCBJbmMxMzAxBgNVBAMTKkRpZ2lDZXJ0IEdsb2JhbCBH
            MyBUTFMgRUNDIFNIQTM4NCAyMDIwIENBMTB2MBAGByqGSM49AgEGBSuBBAAiA2IA
            BHipnHWuiF1jpK1dhtgQSdavklljQyOF9EhlMM1KNJWmDj7ZfAjXVwUoSJ4Lq+vC
            05ae7UXSi4rOAUsXQ+Fzz21zSDTcAEYJtVZUyV96xxMH0GwYF2zK28cLJlYujQf1
            Z6OCAYIwggF+MBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYDVR0OBBYEFIoj655r1/k3
            XfltITl2mqFn3hCoMB8GA1UdIwQYMBaAFLPbSKT5ocXYrjZBzBFjaWIpvEvGMA4G
            A1UdDwEB/wQEAwIBhjAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwdgYI
            KwYBBQUHAQEEajBoMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2VydC5j
            b20wQAYIKwYBBQUHMAKGNGh0dHA6Ly9jYWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdp
            Q2VydEdsb2JhbFJvb3RHMy5jcnQwQgYDVR0fBDswOTA3oDWgM4YxaHR0cDovL2Ny
            bDMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0R2xvYmFsUm9vdEczLmNybDA9BgNVHSAE
            NjA0MAsGCWCGSAGG/WwCATAHBgVngQwBATAIBgZngQwBAgEwCAYGZ4EMAQICMAgG
            BmeBDAECAzAKBggqhkjOPQQDAwNoADBlAjB+Jlhu7ojsDN0VQe56uJmZcNFiZU+g
            IJ5HsVvBsmcxHcxyeq8ickBCbmWE/odLDxkCMQDmv9auNIdbP2fHHahv1RJ4teaH
            MUSpXca4eMzP79QyWBH/OoUGPB2Eb9P1+dozHKQ=
            -----END CERTIFICATE-----
            -----BEGIN CERTIFICATE-----
            MIICPzCCAcWgAwIBAgIQBVVWvPJepDU1w6QP1atFcjAKBggqhkjOPQQDAzBhMQsw
            CQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3d3cu
            ZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBHMzAe
            Fw0xMzA4MDExMjAwMDBaFw0zODAxMTUxMjAwMDBaMGExCzAJBgNVBAYTAlVTMRUw
            EwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5jb20x
            IDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IEczMHYwEAYHKoZIzj0CAQYF
            K4EEACIDYgAE3afZu4q4C/sLfyHS8L6+c/MzXRq8NOrexpu80JX28MzQC7phW1FG
            fp4tn+6OYwwX7Adw9c+ELkCDnOg/QW07rdOkFFk2eJ0DQ+4QE2xy3q6Ip6FrtUPO
            Z9wj/wMco+I+o0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBhjAd
            BgNVHQ4EFgQUs9tIpPmhxdiuNkHMEWNpYim8S8YwCgYIKoZIzj0EAwMDaAAwZQIx
            AK288mw/EkrRLTnDCgmXc/SINoyIJ7vmiI1Qhadj+Z4y3maTD/HMsQmP3Wyr+mt/
            oAIwOWZbwmSNuJ5Q3KjVSaLtx9zRSX8XAbjIho9OjIgrqJqpisXRAL34VOKa5Vt8
            sycX
            -----END CERTIFICATE-----
    """;

    private static final String VALID_CERT_SUBJECT_DN = "CN=Zowe Service,OU=API Mediation Layer,O=Zowe Sample,L=Prague,ST=Prague,C=CZ";
    private static final String VALID_CERT_ROOT_SUBJECT_DN = "CN=DigiCert Global Root G3,OU=www.digicert.com,O=DigiCert Inc,C=US";
    private static final String VALID_CERT_INT_SUBJECT_DN = "CN=DigiCert Global G3 TLS ECC SHA384 2020 CA1,O=DigiCert Inc,C=US";

    private static final String CERTS_URL = "https://localhost/gateway/certificates";

    @Mock
    private CloseableHttpClient closeableHttpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private HttpEntity responseEntity;
    @Mock
    private ApimlLogger apimlLog;

    private TrustedCertificatesProvider provider;

    @BeforeEach
    void setup() {
        provider = new TrustedCertificatesProvider(closeableHttpClient);
    }

    @AfterEach
    void tearDown() {
        reset(httpResponse);
    }

    @Nested
    class GivenHttpRequest {

        @BeforeEach
        void setUp() {
            HttpClientMockHelper.mockExecuteWithResponse(closeableHttpClient, httpResponse);
        }

        @Nested
        class GivenResponseWithValidCertificate {

            @Test
            void whenGetTrustedCerts_thenCertificatesReturned() throws UnsupportedOperationException {
                HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, VALID_CERTIFICATE);
                var result = provider.getTrustedCerts(CERTS_URL);
                assertNotNull(result);
                assertEquals(1, result.size());

                X509Certificate trustedCert = (X509Certificate) result.get(0);
                assertEquals(VALID_CERT_SUBJECT_DN, trustedCert.getSubjectX500Principal().getName());
            }

            @Test
            void whenGetTrustedCerts_thenMultipleCertificatesReturned() {
                HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, VALID_CERTIFICATE_CHAIN);
                var result = provider.getTrustedCerts(CERTS_URL);
                assertNotNull(result);
                assertEquals(2, result.size());

                var trustedCert1 = (X509Certificate) result.get(0);
                var trustedCert2 = (X509Certificate) result.get(1);

                assertEquals(VALID_CERT_ROOT_SUBJECT_DN, trustedCert2.getSubjectX500Principal().getName());
                assertEquals(VALID_CERT_INT_SUBJECT_DN, trustedCert1.getSubjectX500Principal().getName());
            }

            @Test
            void whenGetTrustedCerts_thenInvalidFormat() {
                ReflectionTestUtils.setField(provider, "apimlLog", apimlLog);
                when(apimlLog.log(eq("org.zowe.apiml.security.common.verify.errorParsingCertificates"), eq(CERTS_URL), anyString())).thenReturn(null);
                HttpClientMockHelper.mockResponse(httpResponse, HttpStatus.SC_OK, VALID_CERTIFICATE_CHAIN.substring(10, VALID_CERTIFICATE_CHAIN.length()));

                var result = provider.getTrustedCerts(CERTS_URL);
                assertTrue(result.isEmpty());
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


    @Nested
    class OnSplitCerts {

        @Nested
        class GivenInvalidFormat {

            @Test
            void thenIOException() {
                var certificatesPemInvalid = """
                    MIID7zCCAtegAwIBAgIED0TPEjANBgkqhkiG9w0BAQsFADB6MQswCQYDVQQGEwJD
                    WjEPMA0GA1UECBMGUHJhZ3VlMQ8wDQYDVQQHEwZQcmFndWUxFDASBgNVBAoTC1pv
                    -----END CERTIFICATE-----
                """;

                var e = assertThrows(IOException.class, () -> provider.splitCerts(certificatesPemInvalid));
                assertEquals("Certificate is not RFC1421 hex-encoded DER bytes", e.getMessage());
            }

        }

        @Nested
        class GivenValidFormat {

            @Test
            void thenSplit() throws IOException {
                var splitCerts = provider.splitCerts(VALID_CERTIFICATE_CHAIN);
                assertEquals(2, splitCerts.size());
                splitCerts.forEach(cert -> {
                    assertTrue(cert.startsWith(BEGIN_CERT + "\n"));
                    assertTrue(cert.endsWith(END_CERT));
                });

            }

        }

    }

}
