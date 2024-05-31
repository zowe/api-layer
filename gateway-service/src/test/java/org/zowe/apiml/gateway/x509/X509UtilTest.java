/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.x509;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.SslInfo;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class X509UtilTest {

    @Mock
    private SslInfo sslInfo;

    @Mock
    private X509Certificate certificate;

    private final X509Certificate[] x509Certificates = new X509Certificate[1];

    @Test
    void givenCorrectCertificate_whenGetCertificate_thenGetCertificate() throws CertificateEncodingException {
        byte[] cert = "cert".getBytes();
        x509Certificates[0] = certificate;
        when(sslInfo.getPeerCertificates()).thenReturn(x509Certificates);
        when(certificate.getEncoded()).thenReturn(cert);

        String encodedCert = X509Util.getEncodedClientCertificate(sslInfo);

        assertEquals(Base64.getEncoder().encodeToString(cert), encodedCert);
    }

    @Test
    void givenNoSSL_whenGetCertificate_thenNull() throws CertificateEncodingException {
        String encodedCert = X509Util.getEncodedClientCertificate(null);

        assertNull(encodedCert);
    }

    @Test
    void givenNoCertificate_whenGetCertificate_thenNull() throws CertificateEncodingException {
        when(sslInfo.getPeerCertificates()).thenReturn(null);
        String encodedCert = X509Util.getEncodedClientCertificate(sslInfo);
        assertNull(encodedCert);

        when(sslInfo.getPeerCertificates()).thenReturn(new X509Certificate[0]);
        encodedCert = X509Util.getEncodedClientCertificate(sslInfo);
        assertNull(encodedCert);
    }

}
