/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
import java.util.Base64;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcceptForwardedCertFilterTest {

    private static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final String ATTR_CLIENT_AUTH = "client.auth.X509Certificate";
    private static final String ATTR_JAVAX_CERT = "javax.servlet.request.X509Certificate";

    /**
     * A real DER-encoded X.509 certificate in Base64 so that {@code getClientCertFromHeader}
     * can parse it successfully.
     */
    private static final String VALID_CERT_BASE64 =
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

    private static Certificate parsedHeaderCert;

    private AcceptForwardedCertFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private CertificateValidator certificateValidator;

    @BeforeAll
    static void parseHeaderCert() throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream stream = new ByteArrayInputStream(Base64.getDecoder().decode(VALID_CERT_BASE64));
        parsedHeaderCert = cf.generateCertificate(stream);
    }

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        certificateValidator = mock(CertificateValidator.class);
        when(certificateValidator.isForwardingEnabled()).thenReturn(false);
        when(certificateValidator.isTrusted(any())).thenReturn(false);
        filter = new AcceptForwardedCertFilter(new HashSet<>(), certificateValidator);
    }

    @Nested
    class GivenNoCertsInRequest {

        @Test
        void thenClientAuthCertIsNull() throws ServletException, IOException {
            filter.doFilter(request, response, chain);

            HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
            assertNotNull(nextRequest);
            assertNull(nextRequest.getAttribute(ATTR_CLIENT_AUTH));
        }

        @Test
        void thenClientCertHeaderIsHidden() throws ServletException, IOException {
            request.addHeader(CLIENT_CERT_HEADER, VALID_CERT_BASE64);

            filter.doFilter(request, response, chain);

            HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
            assertNotNull(nextRequest);
            assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
            assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
        }

        @Test
        void thenOtherHeadersPassThrough() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer token");

            filter.doFilter(request, response, chain);

            HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
            assertNotNull(nextRequest);
            assertEquals("Bearer token", nextRequest.getHeader("Authorization"));
        }
    }

    @Nested
    class GivenCertsInRequest {

        private X509Certificate[] tlsCerts;

        @BeforeEach
        void setUp() {
            tlsCerts = new X509Certificate[]{
                mock(X509Certificate.class),
                mock(X509Certificate.class)
            };
            request.setAttribute(ATTR_JAVAX_CERT, tlsCerts);
        }

        @Nested
        class WhenForwardingDisabled {

            @Test
            void thenClientAuthCertIsSetToTlsCerts() throws ServletException, IOException {
                when(certificateValidator.isForwardingEnabled()).thenReturn(false);

                filter.doFilter(request, response, chain);

                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);
                X509Certificate[] clientAuthCerts = (X509Certificate[]) nextRequest.getAttribute(ATTR_CLIENT_AUTH);
                assertNotNull(clientAuthCerts);
                assertArrayEquals(tlsCerts, clientAuthCerts);
            }

            @Test
            void thenJavaxCertAttributeIsUnchanged() throws ServletException, IOException {
                filter.doFilter(request, response, chain);

                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                X509Certificate[] javaxCerts = (X509Certificate[]) nextRequest.getAttribute(ATTR_JAVAX_CERT);
                assertArrayEquals(tlsCerts, javaxCerts);
            }
        }

        @Nested
        class WhenForwardingEnabledButNotTrusted {

            @BeforeEach
            void setUp() {
                when(certificateValidator.isForwardingEnabled()).thenReturn(true);
                when(certificateValidator.isTrusted(any())).thenReturn(false);
                request.addHeader(CLIENT_CERT_HEADER, VALID_CERT_BASE64);
            }

            @Test
            void thenClientAuthCertIsSetToTlsCerts() throws ServletException, IOException {
                filter.doFilter(request, response, chain);

                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNotNull(nextRequest);
                X509Certificate[] clientAuthCerts = (X509Certificate[]) nextRequest.getAttribute(ATTR_CLIENT_AUTH);
                assertNotNull(clientAuthCerts);
                assertArrayEquals(tlsCerts, clientAuthCerts);
            }

            @Test
            void thenClientCertHeaderIsHidden() throws ServletException, IOException {
                filter.doFilter(request, response, chain);

                HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
            }
        }

        @Nested
        class WhenForwardingEnabledAndTrusted {

            @BeforeEach
            void setUp() {
                when(certificateValidator.isForwardingEnabled()).thenReturn(true);
                when(certificateValidator.isTrusted(any())).thenReturn(true);
            }

            @Nested
            class GivenValidCertInHeader {

                @BeforeEach
                void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, VALID_CERT_BASE64);
                }

                @Test
                void thenClientAuthCertIsSetToHeaderCert() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);

                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);
                    X509Certificate[] clientAuthCerts = (X509Certificate[]) nextRequest.getAttribute(ATTR_CLIENT_AUTH);
                    assertNotNull(clientAuthCerts);
                    assertEquals(1, clientAuthCerts.length);
                    assertSame(parsedHeaderCert, clientAuthCerts[0]);
                }

                @Test
                void thenClientCertHeaderIsHidden() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);

                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }

            @Nested
            class GivenNoCertInHeader {

                @Test
                void thenClientAuthCertIsNotSet() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);

                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);
                    assertNull(nextRequest.getAttribute(ATTR_CLIENT_AUTH));
                }
            }

            @Nested
            class GivenInvalidCertInHeader {

                @BeforeEach
                void setUp() {
                    request.addHeader(CLIENT_CERT_HEADER, "not-a-valid-certificate");
                }

                @Test
                void thenClientAuthCertIsNotSet() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);

                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNotNull(nextRequest);
                    assertNull(nextRequest.getAttribute(ATTR_CLIENT_AUTH));
                }

                @Test
                void thenClientCertHeaderIsHidden() throws ServletException, IOException {
                    filter.doFilter(request, response, chain);

                    HttpServletRequest nextRequest = (HttpServletRequest) chain.getRequest();
                    assertNull(nextRequest.getHeader(CLIENT_CERT_HEADER));
                    assertFalse(nextRequest.getHeaders(CLIENT_CERT_HEADER).hasMoreElements());
                }
            }
        }
    }
}
