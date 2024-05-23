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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zowe.commons.attls.InboundAttls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            byte[] certificate = InboundAttls.getCertificate();
            if (certificate != null && certificate.length > 0) {
                populateRequestWithCertificate(request, certificate);
            }
        } catch (Exception e) {
            logger.error("Not possible to get certificate from AT-TLS context", e);
            AttlsErrorHandler.handleError(response, "Exception reading certificate");
        }
        filterChain.doFilter(request, response);
    }

    public void populateRequestWithCertificate(HttpServletRequest request, byte[] rawCertificate) throws CertificateException {
        var str = String.format("""
            -----BEGIN CERTIFICATE-----
            %s
            -----END CERTIFICATE-----
            """,
            Base64.encodeBase64String(rawCertificate)
        );

        var certificate = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)));
        var certs = new X509Certificate[] { certificate };
        request.setAttribute("jakarta.servlet.request.X509Certificate", certs);
    }

}
