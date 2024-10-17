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

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.InboundAttls;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This filter will add X509 certificate from InboundAttls
 */
@Slf4j
public class AttlsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        byte[] rawCertificate = null;

        try {
            rawCertificate = InboundAttls.getCertificate();
        } catch (Exception e) {
            log.error("Not possible to get rawCertificate from AT-TLS context", e);
            AttlsErrorHandler.handleError(response, "Exception reading rawCertificate");
        }

        if (rawCertificate != null && rawCertificate.length > 0) {
            log.debug("Certificate length: {}", rawCertificate.length);
            try {
                populateRequestWithCertificate(request, rawCertificate);
            } catch (CertificateException ce) {
                log.error("Cannot process rawCertificate: {}\n{}", ce.getMessage(), convert(rawCertificate));
                AttlsErrorHandler.handleError(response, "Exception reading rawCertificate");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String convert(byte[] rawCertificate) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        sb.append(Base64.encodeBase64String(rawCertificate));
        sb.append("\n-----END CERTIFICATE-----");
        return sb.toString();
    }

    public void populateRequestWithCertificate(HttpServletRequest request, byte[] rawCertificate) throws CertificateException {
        X509Certificate certificate = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(new ByteArrayInputStream(convert(rawCertificate).getBytes(StandardCharsets.UTF_8)));
        X509Certificate[] certificates = new X509Certificate[1];
        certificates[0] = certificate;
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);
    }

}
