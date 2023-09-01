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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.security.common.utils.X509Utils;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategorizeCertsFilterTest {

    private static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final String CLIENT_CERT_HEADER_VALUE =
        "MIIEFTCCAv2gAwIBAgIEKWdbVTANBgkqhkiG9w0BAQsFADCBjDELMAkGA1UEBhMC" +
            "Q1oxDTALBgNVBAgTBEJybm8xDTALBgNVBAcTBEJybm8xFDASBgNVBAoTC1pvd2Ug" +
            "U2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMSswKQYDVQQDEyJa" +
            "b3dlIFNlbGYtU2lnbmVkIFVudHJ1c3RlZCBTZXJ2aWNlMB4XDTE4MTIwNzIwMDc1" +
            "MloXDTI4MTIwNDIwMDc1MlowgYwxCzAJBgNVBAYTAkNaMQ0wCwYDVQQIEwRCcm5v" +
            "MQ0wCwYDVQQHEwRCcm5vMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMT" +
            "QVBJIE1lZGlhdGlvbiBMYXllcjErMCkGA1UEAxMiWm93ZSBTZWxmLVNpZ25lZCBV" +
            "bnRydXN0ZWQgU2VydmljZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB" +
            "AJti8p4nr8ztRSbemrAv1ytVLQMbXozhLe3lNaiVADGTFPZYeJ2lDt7oAl238HOY" +
            "ScpOz+JjTeUkL0jsjNYgMhi4J07II/3sJL0SBfVqvvgjUL4BvcpdBl0crSuI/3D4" +
            "OaPue+ZmPFijwdCcw5JbazMoOka/zUwpYYdbwxPUH2BbKfwtmmygX88nkJcRSoQO" +
            "KBdNsUs+QRuUiokZ/FJi7uiOsNZ8eEfQv6qJ7mOJ7l1IrMcNm3jHgodoQi/4jXO1" +
            "np/hZaz/ZDni9kBwcyd64AViB2v7VrrBmjdESt1mtCIMvKMlwAZAqrDO75Q9pepO" +
            "Y7zbN4s9s7IUfyb9431xg2MCAwEAAaN9MHswHQYDVR0lBBYwFAYIKwYBBQUHAwIG" +
            "CCsGAQUFBwMBMA4GA1UdDwEB/wQEAwIE8DArBgNVHREEJDAighVsb2NhbGhvc3Qu" +
            "bG9jYWxkb21haW6CCWxvY2FsaG9zdDAdBgNVHQ4EFgQUIeSN7aNtwH2MnBAGDLre" +
            "TtcSaZ4wDQYJKoZIhvcNAQELBQADggEBAELPbHlG60nO164yrBjZcpQJ/2e5ThOR" +
            "8efXUWExuy/NpwVx0vJg4tb8s9NI3X4pRh3WyD0uGPGkO9w+CAvgUaECePLYjkov" +
            "KIS6Cvlcav9nWqdZau1fywltmOLu8Sq5i42Yvb7ZcPOEwDShpuq0ql7LR7j7P4XH" +
            "+JkA0k9Zi6RfYJAyOOpbD2R4JoMbxBKrxUVs7cEajl2ltckjyRWoB6FBud1IthRR" +
            "mZoPMtlCleKlsKp7yJiE13hpX+qIGnzEQE2gNgQ94dSl4m2xO6pnyDRMAEncmd33" +
            "oehy77omRxNsLzkWe6mjaC8ShMGzG9jYR02iN2h4083/PVXvTZIqwhg=";
    private static Certificate clientCertfromHeader;
    private CategorizeCertsFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private X509Certificate[] certificates;
    private CertificateValidator certificateValidator;

    @BeforeAll
    public static void init() throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream certStream = new ByteArrayInputStream(Base64.getDecoder().decode(CLIENT_CERT_HEADER_VALUE));
        clientCertfromHeader = cf.generateCertificate(certStream);
    }

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        certificateValidator = mock(CertificateValidator.class);
        when(certificateValidator.isForwardingEnabled()).thenReturn(false);
        when(certificateValidator.isTrusted(any())).thenReturn(false);
    }

    @Nested
    class GivenNoPublicKeysInFilter {

        @BeforeEach
        void setUp() {
            filter = new CategorizeCertsFilter(new HashSet<>(), certificateValidator);
        }

        @Nested
        class WhenNoCertificatesInRequest {

            @Test
            void thenRequestNotChanged() throws IOException, ServletException {
                filter.doFilter(request, response, chain);
                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);

                assertNull(nextRequest.getAttribute("javax.servlet.request.X509Certificate"));
                assertNull(nextRequest.getAttribute("client.auth.X509Certificate"));
                assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
            }

            @Nested
            class WhenForwardingEnabled {

                @BeforeEach
                void setUp() {
                    when(certificateValidator.isForwardingEnabled()).thenReturn(true);
                    when(certificateValidator.isTrusted(any())).thenReturn(true);
                }

                @Test
                void thenRequestNotChanged() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    assertNull(nextRequest.getAttribute("javax.servlet.request.X509Certificate"));
                    assertNull(nextRequest.getAttribute("client.auth.X509Certificate"));
                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
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
                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);

                X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(0, apimlCerts.length);

                X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(4, clientCerts.length);
                assertArrayEquals(certificates, clientCerts);

                assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
            }

            @Test
            void thenAllApimlCertificatesWithReversedLogic() throws IOException, ServletException {
                filter.setCertificateForClientAuth(crt -> filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));
                filter.setApimlCertificate(crt -> !filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));

                filter.doFilter(request, response, chain);
                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);

                X509Certificate[] cientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                assertNotNull(cientCerts);
                assertEquals(0, cientCerts.length);

                X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(4, apimlCerts.length);
                assertArrayEquals(certificates, apimlCerts);

                assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
            }

            @Nested
            class WhenCertificateInHeaderAndForwardingEnabled {

                @BeforeEach
                public void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, CLIENT_CERT_HEADER_VALUE);
                    when(certificateValidator.isForwardingEnabled()).thenReturn(true);
                }

                @Test
                void givenTrustedCerts_thenClientCertHeaderAccepted() throws ServletException, IOException {
                    when(certificateValidator.isTrusted(certificates)).thenReturn(true);
                    // when incoming certs are all trusted means that all their public keys are added to the filter
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("foreignCert1"));
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("foreignCert2"));
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("apimlCert1"));
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("apimlCert2"));

                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(4, apimlCerts.length);
                    assertArrayEquals(certificates, apimlCerts);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(1, clientCerts.length);
                    assertSame(clientCertfromHeader, clientCerts[0]);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }

                @Test
                void givenNotTrustedCerts_thenClientCertHeaderIgnored() throws ServletException, IOException {
                    when(certificateValidator.isTrusted(certificates)).thenReturn(false);
                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(0, apimlCerts.length);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(4, clientCerts.length);
                    assertArrayEquals(certificates, clientCerts);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }

            @Nested
            class WhenCertificateInHeaderAndForwardingDisabled {

                @BeforeEach
                public void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, CLIENT_CERT_HEADER_VALUE);
                    when(certificateValidator.isForwardingEnabled()).thenReturn(false);
                }

                @Test
                void thenClientCertHeaderIgnored() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(0, apimlCerts.length);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(4, clientCerts.length);
                    assertArrayEquals(certificates, clientCerts);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }

            @Nested
            class WhenInvalidCertificateInHeaderAndForwardingEnabled {

                @BeforeEach
                public void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, "invalid_cert");
                    when(certificateValidator.isForwardingEnabled()).thenReturn(true);
                    when(certificateValidator.isTrusted(certificates)).thenReturn(true);
                }

                @Test
                void thenCertificateInHeaderIgnored() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(0, apimlCerts.length);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(4, clientCerts.length);
                    assertArrayEquals(certificates, clientCerts);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }
        }

        @Nested
        class WhenOtherHeadersInRequest {

            private static final String COMMON_HEADER = "User-Agent";
            private static final String COMMON_HEADER_VALUE = "dummy";
            @BeforeEach
            void setUp() {
                request.addHeader(COMMON_HEADER, COMMON_HEADER_VALUE);
            }

            @Test
            void thenOtherHeadersPassThrough() throws ServletException, IOException {
                filter.doFilter(request, response, chain);
                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);

                assertEquals(COMMON_HEADER_VALUE, nextRequest.getHeader(COMMON_HEADER));
                assertEquals(COMMON_HEADER_VALUE, nextRequest.getHeaders(COMMON_HEADER).nextElement());
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
            )), certificateValidator);
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
                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);

                X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(2, apimlCerts.length);
                assertSame(certificates[1], apimlCerts[0]);
                assertSame(certificates[3], apimlCerts[1]);

                X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(2, clientCerts.length);
                assertSame(certificates[0], clientCerts[0]);
                assertSame(certificates[2], clientCerts[1]);

                assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
            }

            @Test
            void thenCategorizedCertsWithReversedLogic() throws IOException, ServletException {
                filter.setCertificateForClientAuth(crt -> filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));
                filter.setApimlCertificate(crt -> !filter.getPublicKeyCertificatesBase64().contains(filter.base64EncodePublicKey(crt)));

                filter.doFilter(request, response, chain);
                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);

                X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                assertNotNull(clientCerts);
                assertEquals(2, clientCerts.length);
                assertSame(certificates[1], clientCerts[0]);
                assertSame(certificates[3], clientCerts[1]);

                X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                assertNotNull(apimlCerts);
                assertEquals(2, apimlCerts.length);
                assertSame(certificates[0], apimlCerts[0]);
                assertSame(certificates[2], apimlCerts[1]);

                assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
            }

            @Nested
            class WhenCertificateInHeaderAndForwardingEnabled {

                @BeforeEach
                public void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, CLIENT_CERT_HEADER_VALUE);
                    when(certificateValidator.isForwardingEnabled()).thenReturn(true);
                }

                @Test
                void givenTrustedCerts_thenClientCertHeaderAccepted() throws ServletException, IOException {
                    when(certificateValidator.isTrusted(certificates)).thenReturn(true);
                    // when incoming certs are all trusted means that all their public keys are added to the filter
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("foreignCert1"));
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("foreignCert2"));
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("apimlCert1"));
                    filter.getPublicKeyCertificatesBase64().add(X509Utils.correctBase64("apimlCert2"));

                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(4, apimlCerts.length);
                    assertArrayEquals(certificates, apimlCerts);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(1, clientCerts.length);
                    assertSame(clientCertfromHeader, clientCerts[0]);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }

                @Test
                void givenNotTrustedCerts_thenClientCertHeaderIgnored() throws ServletException, IOException {
                    when(certificateValidator.isTrusted(certificates)).thenReturn(false);
                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(2, apimlCerts.length);
                    assertSame(certificates[1], apimlCerts[0]);
                    assertSame(certificates[3], apimlCerts[1]);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(2, clientCerts.length);
                    assertSame(certificates[0], clientCerts[0]);
                    assertSame(certificates[2], clientCerts[1]);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }

            @Nested
            class WhenCertificateInHeaderAndForwardingDisabled {

                @BeforeEach
                public void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, CLIENT_CERT_HEADER_VALUE);
                    when(certificateValidator.isForwardingEnabled()).thenReturn(false);
                }

                @Test
                void thenClientCertHeaderIgnored() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);
                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);

                    X509Certificate[] apimlCerts = (X509Certificate[]) nextRequest.getAttribute("javax.servlet.request.X509Certificate");
                    assertNotNull(apimlCerts);
                    assertEquals(2, apimlCerts.length);
                    assertSame(certificates[1], apimlCerts[0]);
                    assertSame(certificates[3], apimlCerts[1]);

                    X509Certificate[] clientCerts = (X509Certificate[]) nextRequest.getAttribute("client.auth.X509Certificate");
                    assertNotNull(clientCerts);
                    assertEquals(2, clientCerts.length);
                    assertSame(certificates[0], clientCerts[0]);
                    assertSame(certificates[2], clientCerts[1]);

                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }
        }
    }
}
