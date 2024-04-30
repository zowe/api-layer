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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        sb.append(Base64.encodeBase64String(rawCertificate));
        sb.append("\n-----END CERTIFICATE-----");
        X509Certificate certificate = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(new ByteArrayInputStream(sb.toString().getBytes()));
        X509Certificate[] certificates = new X509Certificate[1];
        certificates[0] = certificate;
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
    }

}
