/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.utils.X509Utils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategorizeCertsFilterTest {

    private CategorizeCertsFilter filter;
    private ServletRequest request;
    private ServletResponse response;
    private FilterChain chain;
    private X509Certificate[] certificates;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Nested
    class GivenNoPublicKeysInFilter {

        @BeforeEach
        void setUp() {
            filter = new CategorizeCertsFilter(Collections.emptySet(), null);
        }

        @Nested
        class WhenNoCertificatesInRequest {

            @Test
            void thenRequestNotChanged() throws IOException, ServletException {
                filter.doFilter(request, response, chain);

                assertNull(request.getAttribute("javax.servlet.request.X509Certificate"));
                assertNull(request.getAttribute("client.auth.X509Certificate"));
                verify(chain, times(1)).doFilter(request, response);
            }
        }

        @Nested
        class WhenCertificatesInRequest {

            @BeforeEach
            void setUp() {
                certificates = new X509Certificate[]{
                    X509Utils.getCertificate(X509Utils.correctBase64("foreignCert1")),
                    X509Utils.getCertificate(X509Utils.correctBase64("apimlCert1")),
                    X509Utils.getCertificate(X509Utils.correctBase64("foreignCert2")),
                    X509Utils.getCertificate(X509Utils.correctBase64("apimlCert2"))
                };
                request.setAttribute("javax.servlet.request.X509Certificate", certificates);
            }

            @Test
            void thenAllClientCertificates() throws IOException, ServletException {
                filter.doFilter(request, response, chain);

                X509Certificate[] apimlCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(0, apimlCerts.length);

                X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(4, clientCerts.length);
                assertArrayEquals(certificates, clientCerts);

                verify(chain, times(1)).doFilter(request, response);
            }

            @Test
            void thenAllApimlCertificatesWithReversedLogic() throws IOException, ServletException {
                filter.setCertificateForClientAuth(crt -> filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));
                filter.setNotCertificateForClientAuth(crt -> !filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));

                filter.doFilter(request, response, chain);

                X509Certificate[] cientCerts = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
                assertNotNull(cientCerts);
                assertEquals(0, cientCerts.length);

                X509Certificate[] apimlCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(4, apimlCerts.length);
                assertArrayEquals(certificates, apimlCerts);

                verify(chain, times(1)).doFilter(request, response);
            }
        }
    }

    @Nested
    class GivenPublicKeysInFilter {

        @BeforeEach
        void setUp() {
            filter = new CategorizeCertsFilter(new HashSet<>(Arrays.asList(
                X509Utils.correctBase64("apimlCert1"),
                X509Utils.correctBase64("apimlCert2")
            )), null);
        }

        @Nested
        class WhenCertificatesInRequest {

            @BeforeEach
            void setUp() {
                certificates = new X509Certificate[]{
                    X509Utils.getCertificate(X509Utils.correctBase64("foreignCert1")),
                    X509Utils.getCertificate(X509Utils.correctBase64("apimlCert1")),
                    X509Utils.getCertificate(X509Utils.correctBase64("foreignCert2")),
                    X509Utils.getCertificate(X509Utils.correctBase64("apimlCert2"))
                };
                request.setAttribute("javax.servlet.request.X509Certificate", certificates);
            }

            @Test
            void thenCategorizedCerts() throws IOException, ServletException {
                filter.doFilter(request, response, chain);

                X509Certificate[] apimlCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(2, apimlCerts.length);
                assertSame(certificates[1], apimlCerts[0]);
                assertSame(certificates[3], apimlCerts[1]);

                X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(2, clientCerts.length);
                assertSame(certificates[0], clientCerts[0]);
                assertSame(certificates[2], clientCerts[1]);

                verify(chain, times(1)).doFilter(request, response);
            }

            @Test
            void thenCategorizedCertsWithReversedLogic() throws IOException, ServletException {
                filter.setCertificateForClientAuth(crt -> filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));
                filter.setNotCertificateForClientAuth(crt -> !filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));

                filter.doFilter(request, response, chain);

                X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(2, clientCerts.length);
                assertSame(certificates[1], clientCerts[0]);
                assertSame(certificates[3], clientCerts[1]);

                X509Certificate[] apimlCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(2, apimlCerts.length);
                assertSame(certificates[0], apimlCerts[0]);
                assertSame(certificates[2], apimlCerts[1]);

                verify(chain, times(1)).doFilter(request, response);
            }
        }

        @Nested
        class WhenCertificatesInRequestHeader {
            MockHttpServletRequest request = new MockHttpServletRequest();
            String cert = "-----BEGIN CERTIFICATE-----\n" +
                "MIIEFTCCAv2gAwIBAgIEKWdbVTANBgkqhkiG9w0BAQsFADCBjDELMAkGA1UEBhMC\n" +
                "Q1oxDTALBgNVBAgTBEJybm8xDTALBgNVBAcTBEJybm8xFDASBgNVBAoTC1pvd2Ug\n" +
                "U2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMSswKQYDVQQDEyJa\n" +
                "b3dlIFNlbGYtU2lnbmVkIFVudHJ1c3RlZCBTZXJ2aWNlMB4XDTE4MTIwNzIwMDc1\n" +
                "MloXDTI4MTIwNDIwMDc1MlowgYwxCzAJBgNVBAYTAkNaMQ0wCwYDVQQIEwRCcm5v\n" +
                "MQ0wCwYDVQQHEwRCcm5vMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMT\n" +
                "QVBJIE1lZGlhdGlvbiBMYXllcjErMCkGA1UEAxMiWm93ZSBTZWxmLVNpZ25lZCBV\n" +
                "bnRydXN0ZWQgU2VydmljZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                "AJti8p4nr8ztRSbemrAv1ytVLQMbXozhLe3lNaiVADGTFPZYeJ2lDt7oAl238HOY\n" +
                "ScpOz+JjTeUkL0jsjNYgMhi4J07II/3sJL0SBfVqvvgjUL4BvcpdBl0crSuI/3D4\n" +
                "OaPue+ZmPFijwdCcw5JbazMoOka/zUwpYYdbwxPUH2BbKfwtmmygX88nkJcRSoQO\n" +
                "KBdNsUs+QRuUiokZ/FJi7uiOsNZ8eEfQv6qJ7mOJ7l1IrMcNm3jHgodoQi/4jXO1\n" +
                "np/hZaz/ZDni9kBwcyd64AViB2v7VrrBmjdESt1mtCIMvKMlwAZAqrDO75Q9pepO\n" +
                "Y7zbN4s9s7IUfyb9431xg2MCAwEAAaN9MHswHQYDVR0lBBYwFAYIKwYBBQUHAwIG\n" +
                "CCsGAQUFBwMBMA4GA1UdDwEB/wQEAwIE8DArBgNVHREEJDAighVsb2NhbGhvc3Qu\n" +
                "bG9jYWxkb21haW6CCWxvY2FsaG9zdDAdBgNVHQ4EFgQUIeSN7aNtwH2MnBAGDLre\n" +
                "TtcSaZ4wDQYJKoZIhvcNAQELBQADggEBAELPbHlG60nO164yrBjZcpQJ/2e5ThOR\n" +
                "8efXUWExuy/NpwVx0vJg4tb8s9NI3X4pRh3WyD0uGPGkO9w+CAvgUaECePLYjkov\n" +
                "KIS6Cvlcav9nWqdZau1fywltmOLu8Sq5i42Yvb7ZcPOEwDShpuq0ql7LR7j7P4XH\n" +
                "+JkA0k9Zi6RfYJAyOOpbD2R4JoMbxBKrxUVs7cEajl2ltckjyRWoB6FBud1IthRR\n" +
                "mZoPMtlCleKlsKp7yJiE13hpX+qIGnzEQE2gNgQ94dSl4m2xO6pnyDRMAEncmd33\n" +
                "oehy77omRxNsLzkWe6mjaC8ShMGzG9jYR02iN2h4083/PVXvTZIqwhg=\n" +
                "-----END CERTIFICATE-----\n";
            @Test
            void thenAllClientCertificates() throws IOException, ServletException {
                ReflectionTestUtils.setField(filter, "x509AuthViaHeader", true);
                X509Certificate certificate = X509Utils.getCertificate(X509Utils.correctBase64(cert));
                request.addHeader("x-auth-source", filter.base64EncodePublicKey(certificate));
                filter.doFilter(request, response, chain);

                X509Certificate[] apimlCerts = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(0, apimlCerts.length);

                X509Certificate[] clientCerts = (X509Certificate[]) request.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(1, clientCerts.length);

                verify(chain, times(1)).doFilter(request, response);
            }

        }
    }
}
