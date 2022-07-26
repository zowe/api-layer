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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.mock.web.MockHttpServletRequest;
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
            filter = new CategorizeCertsFilter(Collections.emptySet());
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
            )));
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
    }
}
