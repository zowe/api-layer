/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.InboundAttls;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This filter will add X509 certificate from InboundAttls
 */
public class AttlsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (InboundAttls.getCertificate() != null && InboundAttls.getCertificate().length > 0) {
                try {
                    populateRequestWithCertificate(request, InboundAttls.getCertificate());
                } finally {
                    InboundAttls.clean();
                }
            }
        } catch (Exception e) {
            logger.error("Not possible to get certificate from AT-TLS context", e);
            AttlsErrorHandler.handleError(response, "Exception reading certificate");
        }
        filterChain.doFilter(request, response);
    }

    public void populateRequestWithCertificate(HttpServletRequest request, byte[] rawCertificate) throws CertificateException {
        byte[] encodedCert = Base64.encodeBase64(rawCertificate, false);
        String s = new String(encodedCert);
        s = "-----BEGIN CERTIFICATE-----\n" + s + "\n-----END CERTIFICATE-----";
        X509Certificate certificate = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(new ByteArrayInputStream(s.getBytes()));
        X509Certificate[] certificates = new X509Certificate[1];
        certificates[0] = certificate;
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
    }

}
